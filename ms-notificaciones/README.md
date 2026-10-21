# ms-notificaciones

Microservicio **consumidor** del sistema Biblioteca (proyecto **DSY1107 - Desarrollo Cloud Native I**). Escucha los eventos de préstamos y de stock que publican `ms-prestamo` y `ms-catalogo` en RabbitMQ y genera las notificaciones y alertas correspondientes.

No expone API REST (no tiene puerto HTTP): solo consume mensajes.

- Contrato de eventos: [`docs/contratos/eventos-rabbitmq.md`](../docs/contratos/eventos-rabbitmq.md)

## Stack

- Java 21 · Spring Boot 4.1
- Spring AMQP (RabbitMQ), Jackson 3
- Sin base de datos: la idempotencia se lleva en memoria

## Arquitectura

```
ms-prestamo --prestamo.creado / prestamo.devuelto--> biblioteca.events (topic) --prestamo.*--> notificaciones.queue --+
                                                                                                                      +--> ms-notificaciones
ms-catalogo --stock.agotado------------------------> biblioteca.direct (direct) --stock.agotado--> alertas.stock.queue --+

Mensajes que fallan --> biblioteca.dlx (direct) --> notificaciones.queue.dlq / alertas.stock.queue.dlq --> DeadLetterListener (registra el error)
```

## Topología de RabbitMQ

Este servicio declara la topología que consume (los argumentos de cada cola deben ser idénticos en todos los servicios que la declaran; si no, RabbitMQ responde `PRECONDITION_FAILED`).

| Elemento | Nombre | Tipo / binding |
|---|---|---|
| Exchange de eventos | `biblioteca.events` | topic, durable |
| Exchange de alertas | `biblioteca.direct` | direct, durable |
| Exchange de fallidos | `biblioteca.dlx` | direct, durable |
| Cola de notificaciones | `notificaciones.queue` | quorum, binding `prestamo.*` en `biblioteca.events` |
| Cola de alertas de stock | `alertas.stock.queue` | quorum, binding `stock.agotado` en `biblioteca.direct` |
| DLQ de notificaciones | `notificaciones.queue.dlq` | quorum, binding con su propio nombre en `biblioteca.dlx` |
| DLQ de alertas | `alertas.stock.queue.dlq` | quorum, binding con su propio nombre en `biblioteca.dlx` |

Los nombres se leen de `application.properties` (`app.rabbit.*`) y la topología se declara en `messaging/config/RabbitTopologyConfig.java`.

## Eventos que procesa

| Evento | Cola | Resultado |
|---|---|---|
| `prestamo.creado` | `notificaciones.queue` | Notificación de préstamo creado (usuario, libro y fecha de devolución estimada) |
| `prestamo.devuelto` | `notificaciones.queue` | Notificación de devolución |
| `stock.agotado` | `alertas.stock.queue` | Alerta de libro sin unidades disponibles |

## Manejo de errores (ACK manual, reintentos y DLQ)

El reconocimiento es **manual** (`acknowledge-mode=manual`, prefetch 10). El flujo de cada mensaje está en `messaging/processor/MessageProcessor.java`:

1. **Validación** (`EventoValidator`). Si el mensaje no es JSON, no es un objeto, o le falta un campo obligatorio o tiene un valor inválido (`eventId` que no es UUID, ids no numéricos, fechas que no son ISO-8601 UTC), se rechaza de inmediato con **NACK sin reencolar**, que lo envía a la DLQ. No se reintenta, porque reintentar un mensaje inválido no lo arregla.
2. **Procesamiento con reintentos.** Si el servicio falla, se reintenta hasta **3 veces** con esperas de **1, 2 y 4 segundos**, con un `WARN` por cada reintento.
3. **Agotados los reintentos**, se registra un `ERROR` y se hace **NACK sin reencolar** hacia la DLQ.
4. **Éxito**: `ACK`.
5. **Idempotencia:** si llega un `eventId` ya procesado, se ignora (`Evento duplicado ignorado`) y se confirma.

`DeadLetterListener` consume ambas DLQ y registra cada mensaje fallido con su `eventId`, la cola de origen, la causa y el cuerpo:

```
ERROR DLQ: eventId=..., colaOrigen=notificaciones.queue, causa=rejected, cuerpo={...}
```

Como ese listener confirma los mensajes de la DLQ, las DLQ de este servicio se ven en 0 en la consola de RabbitMQ mientras el servicio está corriendo. Para ver un mensaje dentro de la DLQ, detén este servicio antes de provocar el fallo.

## Configuración (variables de entorno)

Copia `.env.example` a `.env` en esta carpeta y completa los valores (el `.env` no se versiona, ver `.gitignore`):

| Variable | Descripción |
|---|---|
| `RABBIT_ADDRESSES` | Nodos del clúster, ej. `localhost:5672,localhost:5673` |
| `RABBIT_USERNAME` | Usuario de RabbitMQ |
| `RABBIT_PASSWORD` | Clave de RabbitMQ |

## Ejecutar

1. Levantar RabbitMQ desde la raíz del repo: `docker compose up -d` (consola de administración en http://localhost:15672).
2. Levantar este servicio **antes** que los productores (`ms-catalogo`, `ms-prestamo`), porque es quien declara las colas; los eventos publicados antes de que existan las colas se pierden.

```bash
./mvnw spring-boot:run
```

## Probar los escenarios

Se publican a mano desde la consola de RabbitMQ: **Exchanges**, `biblioteca.events`, **Publish message**, con routing key `prestamo.creado`.

| Escenario | Qué publicar | Resultado esperado |
|---|---|---|
| Flujo normal | Crear un préstamo desde `ms-prestamo` | `INFO NOTIFICACIÓN: préstamo ... creado` |
| JSON roto | Payload `{esto no es json` | `ERROR Mensaje inválido ... causa=JSON inválido` y luego `ERROR DLQ`; sin reintentos |
| Campo faltante | Evento sin `titulo` | `ERROR Mensaje inválido ... causa=Campo obligatorio inválido: titulo` y `ERROR DLQ` |
| Evento repetido | El mismo `eventId` dos veces | La segunda vez: `Evento duplicado ignorado` |
| Reintentos hasta la DLQ | Evento válido con `"titulo": "FALLAR"` | 3 `WARN Reintento n/3` (1, 2 y 4 s), `ERROR Evento rechazado` y `ERROR DLQ` |
| Stock agotado | En `biblioteca.direct`, routing key `stock.agotado` | `WARN ALERTA STOCK: libro ... sin unidades disponibles` |

> El título `FALLAR` es un gatillo de prueba que lanza un fallo simulado en `NotificacionService`, para poder demostrar los reintentos y la DLQ. No tiene efecto en un uso normal.

Ejemplo de payload válido de `prestamo.creado` (usa un `eventId` distinto en cada prueba):

```json
{
  "eventId": "11111111-1111-4111-8111-111111111111",
  "tipo": "prestamo.creado",
  "prestamoId": 1,
  "libroId": 1,
  "titulo": "Fahrenheit 451",
  "usuario": "usuario-sub",
  "fechaPrestamo": "2026-10-22T15:30:00Z",
  "fechaDevolucionEstimada": "2026-11-05T15:30:00Z",
  "estado": "ACTIVO"
}
```

## Estructura

```
src/main/java/.../ms_notificaciones/
├── messaging/
│   ├── config/      RabbitTopologyConfig       (exchanges, colas, DLQ y bindings)
│   ├── event/       EventoValidator, EventoInvalidoException
│   ├── listener/    PrestamoListener, StockListener, DeadLetterListener
│   └── processor/   MessageProcessor           (validación, reintentos, ACK/NACK)
└── service/         NotificacionService        (genera la notificación; idempotencia en memoria)
```
