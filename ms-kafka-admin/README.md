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