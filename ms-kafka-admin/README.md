# ms-kafka-admin

Microservicio de administración de Apache Kafka. Expone una API REST para gestionar tópicos (listar, inspeccionar, crear, actualizar configuración y eliminar) y consultar consumer groups (miembros, offsets y lag), protegida con JWT de Microsoft Entra ID (rol `ADMIN`).

- Puerto: `8084`

## Estructura

| Paquete | Responsabilidad |
|---|---|
| `config` | `KafkaAdminConfig`: configuración de `KafkaAdmin` y bean `AdminClient` con timeouts controlados. |
| `controller` | Endpoints REST (`TopicController`, `ConsumerGroupController`). |
| `service` | `KafkaAdminService`: lógica de administración sobre Apache Kafka usando `AdminClient`. |
| `dto` | Requests con validaciones y respuestas de tópicos y consumer groups. |
| `exception` | Excepciones propias y `ApiExceptionHandler` (formato de error único). |
| `security` | Validación del JWT (issuer, audience), conversión de roles y restricción a `ROLE_ADMIN`. |

## Requisitos

- Java 21
- Clúster de Apache Kafka accesible (`docker compose -f docker-compose.kafka.yml up -d`)
- Un token de Entra ID con el rol `ADMIN`

## Configuración

Copia `.env.example` a `.env` en esta carpeta y completa los valores:

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers del clúster, separados por coma | `localhost:9092,localhost:9093,localhost:9094` |
| `KAFKA_ADMIN_TIMEOUT_MS` | Timeout en ms para operaciones de `AdminClient` | `5000` |
| `JWT_ISSUER` | Emisor esperado del token (`iss`) | Entra ID común |
| `JWT_AUDIENCE` | Audiencia esperada (`aud`) | `api://placeholder` |
| `JWT_JWK_SET_URI` | URL de las claves públicas de Entra ID | Entra ID común |

## Ejecución

```powershell
cd ms-kafka-admin
.\mvnw.cmd spring-boot:run
```

Pruebas:

```powershell
.\mvnw.cmd test
```

## Endpoints

Todos requieren `Authorization: Bearer <token>` con rol `ADMIN`. Los nombres deben cumplir `^[a-zA-Z0-9._-]{1,100}$`.

| Método | Ruta | Descripción | Éxito |
|---|---|---|---|
| GET | `/topics` | Lista los tópicos del clúster | 200 |
| GET | `/topics/{name}` | Detalle de particiones y configuración de un tópico | 200 |
| POST | `/topics` | Crea un tópico (`partitions >= 1`, `replicationFactor` entre `1` y `3`) | 201 |
| PUT | `/topics/{name}/config` | Actualiza configuraciones del tópico (`retention.ms`, `cleanup.policy`, etc.) | 200 |
| DELETE | `/topics/{name}` | Elimina un tópico | 204 |
| GET | `/consumer-groups` | Lista los consumer groups y su estado | 200 |
| GET | `/consumer-groups/{id}` | Detalle de miembros, offsets comprometidos y lag por partición | 200 |

### Ejemplo de creación de tópico

```http
POST /topics
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "notificaciones.eventos",
  "partitions": 3,
  "replicationFactor": 3,
  "retentionMs": 604800000,
  "cleanupPolicy": "delete"
}
```

### Ejemplo de actualización de configuración

```http
PUT /topics/notificaciones.eventos/config
Authorization: Bearer <token>
Content-Type: application/json

{
  "retentionMs": 86400000,
  "cleanupPolicy": "delete"
}
```

## Errores

| Código | Cuándo |
|---|---|
| 400 | Falla la validación del cuerpo/parámetros o configuración inválida en Kafka |
| 401 | Token ausente o inválido |
| 403 | Token válido sin rol `ADMIN` |
| 404 | El tópico o consumer group no existe |
| 409 | El tópico ya existe |
| 503 | No hay conexión con el clúster de Kafka |