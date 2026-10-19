# ms-catalogo

Microservicio de catálogo de libros del sistema de biblioteca (proyecto **DSY1107 - Desarrollo Cloud Native I**). Administra los libros y su stock, y **publica eventos a RabbitMQ** cuando un libro se crea, se elimina o se agota.

Actúa como **OAuth 2.0 Resource Server**: no gestiona usuarios ni contraseñas ni emite tokens. Valida los Access Tokens JWT emitidos por **Microsoft Entra ID** y autoriza según scopes y roles.

- Puerto: `8080`
- Contratos: [`docs/contratos/api-rest.md`](../docs/contratos/api-rest.md) y [`docs/contratos/eventos-rabbitmq.md`](../docs/contratos/eventos-rabbitmq.md)

## Stack

- Java 21 · Spring Boot 4.1
- Spring Web, Spring Data JPA, Spring Security (OAuth2 Resource Server)
- Spring AMQP (RabbitMQ)
- MySQL (AWS RDS)
- JUnit 5 + MockMvc + Spring Security Test

## Arquitectura

```
Frontend (React) → API Gateway (valida JWT) → ms-catalogo (EC2, vuelve a validar JWT) → RDS MySQL
                                                   │
                                                   └── publica eventos → RabbitMQ → ms-notificaciones / ms-historial
```

El servicio valida el JWT de forma independiente al Gateway (defensa en profundidad): firma, issuer, audience y expiración.

## Dominio

```
Libro(id, titulo, autor, isbn, stock)
```

## Endpoints

| Ruta | Método | Acceso |
|---|---|---|
| `/public/libros` | GET | Público |
| `/api/libros/{id}` | GET | JWT válido |
| `/api/write/libros/{id}/stock/reservar` | POST | Scope `prestamo.write` (uso interno de ms-prestamo) |
| `/api/write/libros/{id}/stock/liberar` | POST | Scope `prestamo.write` (uso interno de ms-prestamo) |
| `/api/admin/libros` | POST | Rol `ADMIN` |
| `/api/admin/libros/{id}` | DELETE | Rol `ADMIN` |

Detalle de cuerpos y códigos de respuesta en el contrato de API.

## Mensajería (RabbitMQ)

Eventos que publica este servicio:

| Evento | Routing key | Exchange | Cuándo |
|---|---|---|---|
| `libro.creado` | `libro.creado` | `biblioteca.events` (topic) | Se crea un libro |
| `libro.eliminado` | `libro.eliminado` | `biblioteca.events` (topic) | Se elimina un libro |
| `stock.agotado` | `stock.agotado` | `biblioteca.direct` (direct) | El stock llega a 0 al reservar |

Organización del código en `messaging/`:

| Paquete | Contenido |
|---|---|
| `config` | `MessagingProperties` (nombres desde `application.properties`), `RabbitBaseConfig` (exchanges, DLX, conversor JSON), `HistorialLibroConfig` y `AlertasStockConfig` (colas, DLQ y bindings por caso de uso) |
| `event` | `LibroEvento` (payload de los eventos) |
| `publisher` | `LibroEventPublisher` (publica los eventos, sin lógica de negocio) |

Este servicio declara la topología que usa (`historial.queue` con binding `libro.*`, `alertas.stock.queue` con binding `stock.agotado`, cada una con su DLQ). Los argumentos de cada cola deben ser idénticos en todos los servicios que la declaran; si no, RabbitMQ responde `PRECONDITION_FAILED`.

## Configuración (variables de entorno)

Crea un archivo `.env` en esta carpeta (no se versiona, ver `.gitignore`):

```dotenv
DB_URL=jdbc:mysql://<endpoint-rds>:3306/biblioteca
DB_USERNAME=admin
DB_PASSWORD=<password>

JWT_ISSUER=https://login.microsoftonline.com/<TENANT_ID>/v2.0
JWT_AUDIENCE=<API_CLIENT_ID>
JWT_JWK_SET_URI=https://login.microsoftonline.com/<TENANT_ID>/discovery/v2.0/keys

RABBIT_ADDRESSES=localhost:5672,localhost:5673
RABBIT_USERNAME=<usuario>
RABBIT_PASSWORD=<clave>
```

Spring Boot las carga vía `spring.config.import=optional:file:.env[.properties]`. Los nombres de exchanges, colas y routing keys están en `application.properties` bajo `app.messaging.*`.

## Ejecutar en local

```bash
./mvnw spring-boot:run
```

Requiere el `.env`, acceso de red a RDS (agregar tu IP al Security Group) y RabbitMQ en ejecución.

## Ejecutar los tests

```bash
./mvnw test
```

Los tests usan el perfil `application-test.properties` (H2 en memoria y JWT simulado con `spring-security-test`); no requieren `.env` ni conexión real a Entra ID o RDS.

## Despliegue

- **Servicios**: instancia EC2 con `docker-compose` (servicios + clúster RabbitMQ de 2 nodos).
- **Base de datos**: RDS MySQL en la misma VPC.
- **API Gateway**: HTTP API con JWT Authorizer, valida issuer y audience antes de reenviar.

## Qué NO hace este servicio (a propósito)

- No implementa `/login` ni gestiona contraseñas.
- No genera ni firma Access Tokens.
- No tiene ningún `client_secret`.

Esas responsabilidades son de Microsoft Entra ID.

## Repos relacionados

- Frontend: [biblioteca-frontend](https://github.com/Libro-Prestamo/biblioteca-frontend)