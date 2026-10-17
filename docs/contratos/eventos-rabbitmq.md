# Contrato de eventos — RabbitMQ

Define qué mensajes circulan, quién los publica, quién los consume y cómo se manejan los errores. Es el acuerdo entre servicios: si un payload cambia, se cambia aquí primero.

## 1. Topología

### Exchanges

| Exchange | Tipo | Durable | Quién publica |
|---|---|---|---|
| `biblioteca.events` | topic | sí | ms-prestamo, ms-catalogo |
| `biblioteca.direct` | direct | sí | ms-catalogo |
| `biblioteca.dlx` | direct | sí | (RabbitMQ, al rechazar mensajes) |

### Colas y bindings

| Cola | Exchange | Routing key | Consumidor | DLQ |
|---|---|---|---|---|
| `notificaciones.queue` | `biblioteca.events` | `prestamo.*` | ms-notificaciones | `notificaciones.queue.dlq` |
| `historial.queue` | `biblioteca.events` | `prestamo.*`, `libro.*` | ms-historial | `historial.queue.dlq` |
| `alertas.stock.queue` | `biblioteca.direct` | `stock.agotado` | ms-notificaciones | `alertas.stock.queue.dlq` |

Las DLQ se enlazan a `biblioteca.dlx` con la routing key igual al nombre de la DLQ. Todas las colas son **quorum** y durables.

### Quién declara qué

La topología (exchanges, colas, bindings, DLQ) se declara con beans de Spring AMQP en la `@Configuration` de **cada servicio que la usa**, con los mismos nombres. Declarar lo mismo dos veces con los mismos argumentos es inofensivo. Con argumentos distintos RabbitMQ lanza `PRECONDITION_FAILED`, así que los argumentos (`x-dead-letter-exchange`, `x-queue-type`) tienen que ser idénticos en todos los servicios.

## 2. Eventos

### Convenciones para todos los mensajes

- Cuerpo en **JSON UTF-8**, `content_type: application/json`.
- Fechas en **ISO-8601 UTC** (`2026-10-22T15:30:00Z`).
- Propiedades AMQP: `message_id` = `eventId`, `type` = routing key, `timestamp` = momento de publicación.
- Todo evento trae `eventId` (UUID) y `tipo`. El consumidor usa `eventId` para ser **idempotente**: si recibe dos veces el mismo `eventId`, procesa una sola.
- Mensajes persistentes (`delivery_mode = 2`).
- `usuario` es el claim **`sub`** del JWT (un identificador opaco de Entra ID, no el correo). Es lo que devuelve `authentication.getName()` en Spring Security.
- Los campos se pueden **agregar**, pero no renombrar ni quitar sin acordarlo en el equipo. Los consumidores ignoran campos desconocidos.

### `prestamo.creado`

- **Exchange:** `biblioteca.events` · **Publica:** ms-prestamo, después de guardar el préstamo.
- **Consumen:** ms-notificaciones (`notificaciones.queue`), ms-historial (`historial.queue`).

```json
{
  "eventId": "a3f1c2e0-7b4d-4a1e-9c3f-1234567890ab",
  "tipo": "prestamo.creado",
  "prestamoId": 101,
  "libroId": 5,
  "titulo": "Cien años de soledad",
  "usuario": "bM3xQ7f2kLw9TnR8vYcZ1aPdE5uHgJ0sXoI4tKyWq6U",
  "fechaPrestamo": "2026-10-22T15:30:00Z",
  "fechaDevolucionEstimada": "2026-11-05T15:30:00Z",
  "estado": "ACTIVO"
}
```

### `prestamo.devuelto`

- **Exchange:** `biblioteca.events` · **Publica:** ms-prestamo, después de registrar la devolución.
- **Consumen:** ms-notificaciones, ms-historial.

```json
{
  "eventId": "b7d4e1f2-9c3a-4e2d-8f1b-0987654321cd",
  "tipo": "prestamo.devuelto",
  "prestamoId": 101,
  "libroId": 5,
  "titulo": "Cien años de soledad",
  "usuario": "bM3xQ7f2kLw9TnR8vYcZ1aPdE5uHgJ0sXoI4tKyWq6U",
  "fechaDevolucionReal": "2026-10-30T12:00:00Z",
  "estado": "DEVUELTO"
}
```

### `libro.creado` y `libro.eliminado`

- **Exchange:** `biblioteca.events` · **Publica:** ms-catalogo (alta o baja desde el endpoint admin).
- **Consume:** ms-historial. No los consume ms-notificaciones, porque su binding es `prestamo.*`.

```json
{
  "eventId": "c9e2a4b1-5d6f-4c8a-b2e3-aabbccddeeff",
  "tipo": "libro.creado",
  "libroId": 12,
  "titulo": "El túnel",
  "isbn": "9788432217579",
  "stock": 4,
  "ocurridoEn": "2026-10-22T14:00:00Z"
}
```

`libro.eliminado` lleva `eventId`, `tipo`, `libroId`, `titulo`, `ocurridoEn`.

### `stock.agotado`

- **Exchange:** `biblioteca.direct` · **Publica:** ms-catalogo, cuando una reserva deja el stock en 0.
- **Consume:** ms-notificaciones (`alertas.stock.queue`).

```json
{
  "eventId": "d1f3b5c7-8e9a-4b0c-a1d2-112233445566",
  "tipo": "stock.agotado",
  "libroId": 5,
  "titulo": "Cien años de soledad",
  "ocurridoEn": "2026-10-22T15:30:01Z"
}
```

## 3. Resumen: quién publica y quién consume

| Evento | Publica | Exchange | Consume |
|---|---|---|---|
| `prestamo.creado` | ms-prestamo | biblioteca.events | ms-notificaciones, ms-historial |
| `prestamo.devuelto` | ms-prestamo | biblioteca.events | ms-notificaciones, ms-historial |
| `libro.creado` | ms-catalogo | biblioteca.events | ms-historial |
| `libro.eliminado` | ms-catalogo | biblioteca.events | ms-historial |
| `stock.agotado` | ms-catalogo | biblioteca.direct | ms-notificaciones |

## 4. Consumo y manejo de errores

Aplica a todos los consumidores (`@RabbitListener`):

| Aspecto | Regla |
|---|---|
| Modo de ACK | **Manual** (`acknowledge-mode: manual`). Se hace `basicAck` solo cuando el procesamiento terminó bien. |
| Reintentos | Hasta **3 intentos** con espera creciente (1 s, 2 s, 4 s). |
| Fallo definitivo | `basicNack(requeue=false)` → el mensaje pasa a la DLQ de esa cola. |
| Mensaje inválido (JSON roto, campo obligatorio ausente) | Sin reintentos: `basicNack(requeue=false)` directo. Reintentar no lo arregla. |
| Log | Cada reintento se registra con `WARN`. Cada mensaje que llega a una DLQ se registra con `ERROR`, con `eventId`, cola de origen y causa. |
| Idempotencia | Se descartan (con ACK) los mensajes con un `eventId` ya procesado. |
| Prefetch | 10 mensajes sin confirmar por consumidor. |

Un listener por DLQ (en el mismo servicio que consume la cola principal) escribe el mensaje muerto en el log. No lo reencola solo: reprocesar un mensaje de una DLQ es una decisión manual.

## 5. Cómo probar la DLQ

1. Publica en `biblioteca.events` un mensaje con routing key `prestamo.creado` y el cuerpo inválido `{"eventId": "x"}` (por ejemplo desde la consola de RabbitMQ, *Exchanges → biblioteca.events → Publish message*).
2. El consumidor lo rechaza sin reintentos.
3. Verifica en la consola que `notificaciones.queue.dlq` tiene 1 mensaje y que el log del servicio muestra la línea `ERROR` con el `eventId`.

## 6. Clúster

RabbitMQ corre con 2 nodos (`rabbit1`, `rabbit2`). Los servicios se conectan con
`spring.rabbitmq.addresses=rabbit1:5672,rabbit2:5672`. Ver `docker-compose.yml`.
