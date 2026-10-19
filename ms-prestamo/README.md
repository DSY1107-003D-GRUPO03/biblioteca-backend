# ms-prestamo

Microservicio de préstamos del sistema Biblioteca (proyecto **DSY1107 - Desarrollo Cloud Native I**). Gestiona la creación y devolución de préstamos, reserva y libera stock de forma **síncrona** en `ms-catalogo` y **publica eventos** a RabbitMQ de forma **asíncrona**.

Actúa como **OAuth 2.0 Resource Server**: no gestiona usuarios ni emite tokens. Valida los Access Tokens JWT emitidos por **Microsoft Entra ID** y toma el usuario del claim `sub` del token.

- Puerto: `8081`
- Contratos: [`docs/contratos/api-rest.md`](../docs/contratos/api-rest.md) y [`docs/contratos/eventos-rabbitmq.md`](../docs/contratos/eventos-rabbitmq.md)

## Stack

- Java 21 · Spring Boot 4.1
- Spring Web, Spring Data JPA, Spring Security (OAuth2 Resource Server), Bean Validation
- Spring AMQP (RabbitMQ)
- MySQL (AWS RDS, base `prestamos`)

## Arquitectura

```
Frontend → API Gateway (valida JWT) → ms-prestamo (vuelve a validar JWT) → RDS MySQL
                                           │
                                           ├── síncrono: reservar/liberar stock → ms-catalogo
                                           │
                                           └── asíncrono: publica en exchange biblioteca.events (topic)
                                                  ├── prestamo.* → notificaciones.queue → ms-notificaciones
                                                  └── prestamo.* → historial.queue      → ms-historial
```

## Dominio

```
Prestamo(id, libroId, titulo, usuario, fechaPrestamo, fechaDevolucionEstimada, fechaDevolucionReal, estado)
estado: ACTIVO | DEVUELTO
```

El `titulo` se guarda al crear el préstamo (viene de ms-catalogo) y viaja en los eventos.

## Endpoints

| Ruta | Método | Acceso | Descripción |
|---|---|---|---|
| `/api/write/prestamos` | POST | Scope `prestamo.write` | Crea un préstamo. Body: `{"libroId": 1}` |
| `/api/write/prestamos/{id}/devolucion` | POST | Scope `prestamo.write` | Registra la devolución |
| `/api/prestamos` | GET | JWT válido | Lista los préstamos del usuario autenticado, del más reciente al más antiguo |

El usuario nunca viaja en el body: se obtiene del claim `sub` del JWT.

| Código | Cuándo |
|---|---|
| 201 | Préstamo creado |
| 200 | Devolución registrada / listado |
| 400 | Body inválido (`libroId` obligatorio) |
| 401 | Token ausente o inválido |
| 403 | Falta el scope `prestamo.write`, o el préstamo pertenece a otro usuario |
| 404 | El libro o el préstamo no existe |
| 409 | El libro no tiene stock, o el préstamo ya fue devuelto |
| 503 | `ms-catalogo` no responde (timeouts de 3 s para conectar y 5 s para leer) |

## Flujo de `POST /api/write/prestamos`

1. **Síncrono:** `POST /api/write/libros/{id}/stock/reservar` en `ms-catalogo`, reenviando el token del usuario. La respuesta trae el libro con su título.
2. Guarda el préstamo con estado `ACTIVO` (14 días de plazo). Si falla el guardado, libera el stock reservado.
3. **Asíncrono:** publica `prestamo.creado` y responde al cliente sin esperar a los consumidores.

Flujo de la devolución: valida que el préstamo sea del usuario, libera el stock en `ms-catalogo`, marca el préstamo como `DEVUELTO` y publica `prestamo.devuelto`.

## Mensajería (RabbitMQ)

Eventos que publica este servicio:

| Evento | Routing key | Exchange | Cuándo |
|---|---|---|---|
| `prestamo.creado` | `prestamo.creado` | `biblioteca.events` (topic) | Se crea un préstamo |
| `prestamo.devuelto` | `prestamo.devuelto` | `biblioteca.events` (topic) | Se devuelve un préstamo |

Este servicio es **solo productor**: declara únicamente el exchange `biblioteca.events`. Las colas, sus DLQ y los bindings los declara cada consumidor (`ms-historial`, `ms-notificaciones`), para que los argumentos de cada cola estén definidos en un solo lugar.

Los nombres (exchange y routing keys) están en `application.properties` bajo `app.rabbit.*`. La configuración está en `config/RabbitConfig.java` y los mensajes viajan como JSON.

### Ejemplo de payload `prestamo.creado`

```json
{
  "eventId": "a3f1c2e0-7b4d-4a1e-9c3f-1234567890ab",
  "tipo": "prestamo.creado",
  "prestamoId": 101,
  "libroId": 5,
  "titulo": "Cien años de soledad",
  "usuario": "8f2c...-sub-del-token",
  "fechaPrestamo": "2026-10-05T15:30:00Z",
  "fechaDevolucionEstimada": "2026-10-19T15:30:00Z",
  "estado": "ACTIVO"
}
```

`prestamo.devuelto` lleva los mismos campos, con `fechaDevolucionReal` en lugar de las fechas de préstamo y plazo.

## Estructura

```
src/main/java/.../ms_prestamo/
├── client/        LibroClient, LibroDTO         (llamadas REST síncronas a ms-catalogo, con timeouts)
├── config/        RabbitConfig                  (exchange, conversor JSON)
├── controller/    PrestamoController
├── event/         PrestamoCreadoEvent, PrestamoDevueltoEvent, PrestamoEventPublisher
├── model/         Prestamo, EstadoPrestamo
├── repository/    PrestamoRepository
├── security/      SecurityConfig, AudienceValidator, AuthoritiesConverter
└── service/       PrestamoService
```

## Configuración (variables de entorno)

Copia `.env.example` a `.env` en esta carpeta y completa los valores (el `.env` no se versiona, ver `.gitignore`):

| Variable | Descripción |
|---|---|
| `DB_URL` | JDBC de la base `prestamos` en RDS (ej. `jdbc:mysql://<endpoint-rds>:3306/prestamos?createDatabaseIfNotExist=true`) |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales de la base |
| `JWT_ISSUER` | `https://login.microsoftonline.com/<TENANT_ID>/v2.0` |
| `JWT_AUDIENCE` | Client ID de la API registrada en Entra ID |
| `JWT_JWK_SET_URI` | `https://login.microsoftonline.com/<TENANT_ID>/discovery/v2.0/keys` |
| `RABBIT_ADDRESSES` | Nodos del clúster, ej. `localhost:5672,localhost:5673` |
| `RABBIT_USERNAME` / `RABBIT_PASSWORD` | Credenciales de RabbitMQ |
| `CATALOGO_URL` | URL de `ms-catalogo` (por defecto `http://localhost:8080`) |

## Ejecutar

Orden de arranque (los eventos publicados antes de que existan las colas se pierden):

1. RabbitMQ, desde la raíz del repo: `docker compose up -d`
   (consola de administración: http://localhost:15672).
2. `ms-historial` y `ms-notificaciones`: crean sus colas.
3. `ms-catalogo` (puerto 8080).
4. Este servicio:

```bash
./mvnw spring-boot:run
```

## Probar

Con un Access Token de Entra ID que incluya el scope `prestamo.write`:

```bash
# crear un préstamo
curl -X POST http://localhost:8081/api/write/prestamos \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"libroId": 1}'

# listar mis préstamos
curl http://localhost:8081/api/prestamos -H "Authorization: Bearer <TOKEN>"

# devolver
curl -X POST http://localhost:8081/api/write/prestamos/1/devolucion \
  -H "Authorization: Bearer <TOKEN>"
```

Sin token, cualquier ruta `/api/**` responde `401`. Después de crear un préstamo, en la consola de RabbitMQ (pestaña **Queues**) se ven los mensajes en `notificaciones.queue` e `historial.queue`, o ya consumidos si los servicios consumidores están corriendo.