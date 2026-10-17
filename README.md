# Sistema Biblioteca — Backend

Backend de microservicios para la gestión de libros y préstamos de una biblioteca. Los servicios se comunican de forma síncrona (REST) cuando necesitan una respuesta inmediata, y de forma asíncrona mediante **RabbitMQ** para los efectos secundarios (notificaciones, historial, alertas). La autenticación se delega en **Microsoft Entra ID** (OAuth2/OIDC) y cada servicio valida el JWT por su cuenta.

## Arquitectura

```
                        Frontend (React) ── login Entra ID ──► JWT
                               │
                               ▼
                    API Gateway (valida JWT)
                               │
        ┌──────────────┬───────┴───────┬─────────────────┐
        ▼              ▼               ▼                 ▼
  ms-catalogo     ms-prestamo    ms-rabbit-admin         ...
        │              │  ▲
        │              │  └── REST: reserva de stock ───┐
        │              ▼                                │
        │      ┌───────────────────────────────────┐    │
        └─────►│      RabbitMQ (clúster, 2 nodos)  │◄───┘
  stock.agotado│  biblioteca.events (topic)        │
               │  biblioteca.direct (direct)       │
               └───────┬───────────────┬───────────┘
                       ▼               ▼
              ms-notificaciones    ms-historial
```

### Servicios

| Servicio | Responsabilidad | Puerto |
|---|---|---|
| `ms-catalogo` | Catálogo de libros y stock. Publica `libro.creado`, `libro.eliminado` y `stock.agotado`. | 8080 |
| `ms-prestamo` | Creación y devolución de préstamos. Reserva y libera stock en `ms-catalogo` por REST y publica `prestamo.creado` y `prestamo.devuelto`. | 8081 |
| `ms-notificaciones` | Consume `notificaciones.queue` y `alertas.stock.queue`. Simula el envío de notificaciones. | — |
| `ms-historial` | Consume `historial.queue`. Registra cada evento y lo expone por REST. | 8082 |
| `ms-rabbit-admin` | API REST para administrar colas, exchanges y bindings de RabbitMQ. | 8083 |

### Comunicación

| Origen → Destino | Tipo | Motivo |
|---|---|---|
| `ms-prestamo` → `ms-catalogo` | REST síncrono | Aceptar o rechazar el préstamo depende del stock en ese momento. |
| `ms-prestamo` → `ms-notificaciones` / `ms-historial` | Evento (RabbitMQ) | Efectos secundarios que no deben bloquear la respuesta al usuario. |
| `ms-catalogo` → `ms-notificaciones` | Evento (RabbitMQ) | Aviso de stock agotado. |
| `ms-catalogo` → `ms-historial` | Evento (RabbitMQ) | Registro de altas y bajas de libros. |

## Mensajería (RabbitMQ)

### Exchanges, colas y bindings

| Exchange | Tipo | Cola | Routing key | Consumidor |
|---|---|---|---|---|
| `biblioteca.events` | topic | `notificaciones.queue` | `prestamo.*` | ms-notificaciones |
| `biblioteca.events` | topic | `historial.queue` | `prestamo.*`, `libro.*` | ms-historial |
| `biblioteca.direct` | direct | `alertas.stock.queue` | `stock.agotado` | ms-notificaciones |

### Dead Letter Queues

Cada cola tiene un *dead-letter exchange* (`biblioteca.dlx`) y su cola de mensajes fallidos:

| Cola | DLQ |
|---|---|
| `notificaciones.queue` | `notificaciones.queue.dlq` |
| `historial.queue` | `historial.queue.dlq` |
| `alertas.stock.queue` | `alertas.stock.queue.dlq` |

**Manejo de errores en los consumidores:**
1. El mensaje se confirma con **ACK manual** solo cuando se procesó correctamente.
2. Si el procesamiento falla, se **reintenta** un número configurable de veces.
3. Si los reintentos se agotan, se hace `basicNack` sin reencolar y el mensaje pasa a la DLQ.
4. Un listener de cada DLQ **registra en el log** el mensaje fallido y el motivo.

### Eventos

| Evento (routing key) | Lo publica | Contenido |
|---|---|---|
| `prestamo.creado` | ms-prestamo | `eventId`, `prestamoId`, `libroId`, `usuario`, `fechaPrestamo`, `fechaDevolucionEstimada`, `estado` |
| `prestamo.devuelto` | ms-prestamo | `eventId`, `prestamoId`, `libroId`, `usuario`, `fechaDevolucionReal`, `estado` |
| `libro.creado` | ms-catalogo | `eventId`, `libroId`, `titulo`, `isbn`, `stock`, `ocurridoEn` |
| `libro.eliminado` | ms-catalogo | `eventId`, `libroId`, `titulo`, `ocurridoEn` |
| `stock.agotado` | ms-catalogo | `eventId`, `libroId`, `titulo`, `ocurridoEn` |

El detalle de cada payload está en [`docs/contratos/eventos-rabbitmq.md`](docs/contratos/eventos-rabbitmq.md).

### Clúster

RabbitMQ corre como un clúster de **2 nodos** (`rabbit1` y `rabbit2`) con colas *quorum*, que se replican entre nodos. Los servicios se conectan a ambos:

```yaml
spring:
  rabbitmq:
    addresses: rabbit1:5672,rabbit2:5672
```

## Seguridad

- Los usuarios se autentican en **Microsoft Entra ID** (flujo Authorization Code + PKCE desde el frontend).
- Cada servicio con REST es un *Resource Server*: valida firma, `issuer`, `audience` y expiración del JWT.
- Autorización por scope y rol:

| Acceso | Requisito |
|---|---|
| `/public/**` | Público |
| `/api/**` | JWT válido |
| `/api/write/**` | Scope `prestamo.write` |
| `/api/admin/**` y `ms-rabbit-admin` | Rol `ADMIN` |

## API REST

Resumen de rutas. El contrato completo (cuerpos, validaciones y códigos de respuesta) está en [`docs/contratos/api-rest.md`](docs/contratos/api-rest.md).

### ms-catalogo

| Ruta | Método | Acceso |
|---|---|---|
| `/public/libros` | GET | Público |
| `/api/libros/{id}` | GET | Autenticado |
| `/api/write/libros/{id}/stock/reservar` | POST | Scope `prestamo.write` (uso interno de ms-prestamo) |
| `/api/write/libros/{id}/stock/liberar` | POST | Scope `prestamo.write` (uso interno de ms-prestamo) |
| `/api/admin/libros` | POST | ADMIN |
| `/api/admin/libros/{id}` | DELETE | ADMIN |

### ms-prestamo

| Ruta | Método | Acceso |
|---|---|---|
| `/api/prestamos` | GET | Autenticado (solo los propios) |
| `/api/write/prestamos` | POST | Scope `prestamo.write` |
| `/api/write/prestamos/{id}/devolucion` | POST | Scope `prestamo.write` |

### ms-historial

| Ruta | Método | Acceso |
|---|---|---|
| `/api/historial` | GET | Autenticado |

### ms-rabbit-admin

| Ruta | Método | Descripción |
|---|---|---|
| `/queues` | POST | Crear una cola |
| `/queues/{name}` | DELETE | Eliminar una cola |
| `/exchanges` | POST | Crear un exchange |
| `/exchanges/{name}` | DELETE | Eliminar un exchange |
| `/bindings` | POST | Crear un binding |
| `/bindings` | DELETE | Eliminar un binding |

Las entradas se validan: el nombre no puede estar vacío y debe cumplir un patrón permitido, el tipo de exchange debe ser `direct`, `topic` o `fanout`, y el binding requiere origen, destino y routing key.

## Estructura del repositorio

```
.
├── ms-catalogo/
├── ms-prestamo/
├── ms-notificaciones/
├── ms-historial/
├── ms-rabbit-admin/
├── docs/
│   ├── guia-de-desarrollo.md
│   └── contratos/
│       ├── api-rest.md
│       └── eventos-rabbitmq.md
├── docker-compose.yml        # clúster de RabbitMQ
└── README.md
```

Cada servicio es un proyecto Spring Boot independiente (Java 21, Maven Wrapper) con su propio `pom.xml`.

## Configuración

Cada servicio lee su configuración de variables de entorno, que se pueden definir en un archivo `.env` en la carpeta del servicio (no se versiona).

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC de la base de datos |
| `DB_USERNAME` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |
| `JWT_ISSUER` | `https://login.microsoftonline.com/<TENANT_ID>/v2.0` |
| `JWT_AUDIENCE` | Client ID de la API en Entra ID |
| `JWT_JWK_SET_URI` | `https://login.microsoftonline.com/<TENANT_ID>/discovery/v2.0/keys` |

Los nombres de colas, exchanges y routing keys están centralizados en el `application.yml` de cada servicio y se declaran como beans en una clase `@Configuration` por caso de uso, separada de la lógica de negocio.

## Ejecución

### 1. RabbitMQ

```bash
docker compose up -d
```

La consola de administración queda en <http://localhost:15672> (sección *Nodes* para ver el clúster).

### 2. Servicios

Desde la carpeta de cada servicio:

```bash
./mvnw spring-boot:run
```

Orden recomendado: RabbitMQ → `ms-catalogo` → `ms-prestamo` → `ms-notificaciones` → `ms-historial` → `ms-rabbit-admin`. Los productores declaran la topología al arrancar, por eso los consumidores deben iniciarse después.

### Despliegue

El `docker-compose.yml` levanta el clúster de RabbitMQ en un host en la nube (EC2). El tráfico entrante pasa por un API Gateway que valida el JWT antes de llegar a los servicios.

## Documentación

| Documento | Contenido |
|---|---|
| [`docs/contratos/api-rest.md`](docs/contratos/api-rest.md) | Rutas, cuerpos, validaciones y códigos de respuesta de cada servicio |
| [`docs/contratos/eventos-rabbitmq.md`](docs/contratos/eventos-rabbitmq.md) | Topología de RabbitMQ, payloads de los eventos y política de errores |
| [`docs/guia-de-desarrollo.md`](docs/guia-de-desarrollo.md) | Estructura de los servicios, puertos, variables de entorno y reglas de trabajo |

## Tecnologías

Java 21 · Spring Boot · Spring AMQP · Spring Security (OAuth2 Resource Server) · Spring Data JPA · MySQL (RDS) · RabbitMQ · Docker · AWS (API Gateway, EC2, RDS) · Microsoft Entra ID
