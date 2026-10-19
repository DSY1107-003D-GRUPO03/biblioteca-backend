# ms-rabbit-admin

Microservicio de administración de RabbitMQ. Expone una API REST para crear y eliminar colas, exchanges y bindings, protegida con JWT de Microsoft Entra ID (rol `ADMIN`).

- Puerto: `8083`
- Contrato completo de la API: [`docs/contratos/api-rest.md`](../docs/contratos/api-rest.md) (sección 7)

## Estructura

| Paquete | Responsabilidad |
|---|---|
| `controller` | Endpoints REST (`QueueController`, `ExchangeController`, `BindingController`). Solo reciben la petición y delegan. |
| `service` | `RabbitAdminService`: toda la lógica de administración sobre RabbitMQ. |
| `dto` | Cuerpos de las peticiones y sus reglas de validación. |
| `exception` | Excepciones propias y `ApiExceptionHandler` (formato de error único). |
| `security` | Validación del JWT (issuer, audience), conversión de roles y reglas de acceso. |

## Requisitos

- Java 21
- RabbitMQ accesible (local o clúster)
- Un token de Entra ID con el rol `ADMIN`

## Configuración

Las variables se leen de un archivo `.env` en esta carpeta (no se sube al repositorio). Copia `.env.example` a `.env` y completa los valores:

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `RABBIT_ADDRESSES` | Nodos del clúster, separados por coma | `localhost:5672` |
| `RABBIT_USERNAME` | Usuario de RabbitMQ | `guest` |
| `RABBIT_PASSWORD` | Clave de RabbitMQ | `guest` |
| `JWT_ISSUER` | Emisor esperado del token (`iss`) | Entra ID común |
| `JWT_AUDIENCE` | Audiencia esperada (`aud`) | `api://placeholder` |
| `JWT_JWK_SET_URI` | URL de las claves públicas de Entra ID | Entra ID común |

## Ejecución

```powershell
cd ms-rabbit-admin
.\mvnw spring-boot:run
```

Pruebas:

```powershell
.\mvnw test
```

## Endpoints

Todos requieren `Authorization: Bearer <token>` con rol `ADMIN`. Los nombres deben cumplir `^[a-zA-Z0-9._-]{1,100}$`.

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| POST | `/queues` | Crea una cola | 201 |
| DELETE | `/queues/{name}` | Elimina una cola | 204 |
| POST | `/exchanges` | Crea un exchange (`direct`, `topic`, `fanout`) | 201 |
| DELETE | `/exchanges/{name}` | Elimina un exchange | 204 |
| POST | `/bindings` | Crea un binding exchange → cola | 201 |
| DELETE | `/bindings` | Elimina un binding (idempotente) | 204 |

Por el API Gateway las rutas llevan el prefijo `/rabbit` (por ejemplo `POST /rabbit/queues`).

### Ejemplo

```http
POST /queues
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "pedidos.queue",
  "durable": true,
  "quorum": true,
  "deadLetterExchange": "biblioteca.dlx",
  "deadLetterRoutingKey": "pedidos.queue.dlq"
}
```

## Errores

Formato único para todos los errores:

```json
{ "status": 404, "error": "Not Found", "mensaje": "La cola no existe: pedidos.queue" }
```

| Código | Cuándo |
|---|---|
| 400 | Falla la validación del cuerpo (agrega la lista `errores` con `campo` y `mensaje`) |
| 401 | Token ausente o inválido |
| 403 | Token válido sin rol `ADMIN` |
| 404 | La cola o el exchange no existen |
| 409 | RabbitMQ rechaza la operación (por ejemplo, argumentos distintos a una cola ya declarada) |
| 503 | No hay conexión con RabbitMQ |