# Biblioteca · Backend

API REST en Spring Boot para el sistema de gestión de biblioteca (dominio **Libro–Préstamo**), desarrollada para la Evaluación Parcial N°1/N°2 de **DSY1107 - Desarrollo Cloud Native I**.

Actúa como **OAuth 2.0 Resource Server**: no gestiona usuarios ni contraseñas ni emite tokens — valida los Access Tokens JWT emitidos por **Microsoft Entra ID** y autoriza el acceso según scopes y roles.

## Stack

- Java 21 · Spring Boot 4.1
- Spring Web, Spring Data JPA, Spring Security (OAuth2 Resource Server)
- MySQL (AWS RDS)
- JUnit 5 + MockMvc + Spring Security Test

## Arquitectura

```
Frontend (React) → API Gateway (AWS, valida JWT) → este backend (EC2, vuelve a validar JWT) → RDS MySQL
```

El backend valida el JWT de forma independiente al Gateway (defensa en profundidad): verifica firma, issuer, audience y expiración, y autoriza por scope/rol.

## Dominio

```
Libro(id, titulo, autor, isbn, stock)
Prestamo(id, libro → FK, usuario, fechaPrestamo, fechaDevolucion, estado)
```

## Endpoints

| Ruta | Método | Acceso |
|---|---|---|
| `/public/libros` | GET | Público |
| `/api/libros/{id}` | GET | Autenticado |
| `/api/prestamos` | GET | Autenticado (devuelve solo los propios) |
| `/api/write/prestamos` | POST | Autenticado + `SCOPE_prestamo.write` |
| `/api/admin/libros` | POST | Autenticado + `ROLE_ADMIN` |
| `/api/admin/libros/{id}` | DELETE | Autenticado + `ROLE_ADMIN` |

## Configuración (variables de entorno)

Crea un archivo `.env` en la raíz del proyecto (no se versiona, ver `.gitignore`):

```dotenv
DB_URL=jdbc:mysql://<endpoint-rds>:3306/biblioteca
DB_USERNAME=admin
DB_PASSWORD=<password>

JWT_ISSUER=https://login.microsoftonline.com/<TENANT_ID>/v2.0
JWT_AUDIENCE=<API_CLIENT_ID>
JWT_JWK_SET_URI=https://login.microsoftonline.com/<TENANT_ID>/discovery/v2.0/keys
```

Spring Boot las carga automáticamente vía `spring.config.import=optional:file:.env[.properties]`.

## Ejecutar en local

```bash
./mvnw spring-boot:run
```

Requiere el `.env` configurado y acceso de red a la instancia RDS (agregar tu IP al Security Group correspondiente).

## Ejecutar los tests

```bash
./mvnw test
```

Los tests usan un perfil separado (`application-test.properties`) con base de datos H2 en memoria y JWT simulado (`spring-security-test`) — no requieren `.env` ni conexión a Entra ID/RDS real.

Cobertura actual:
- Carga del contexto de la aplicación
- Acceso público sin token (200)
- Acceso protegido sin token (401)
- Acceso protegido con token válido pero sin rol (403)

## Despliegue

- **Backend**: instancia EC2 (Amazon Linux), corriendo como servicio `systemd`, con IP elástica fija.
- **Base de datos**: RDS MySQL, misma VPC que la instancia EC2.
- **API Gateway**: HTTP API con JWT Authorizer nativo, validando issuer/audience antes de reenviar al backend.

## Qué NO hace este backend (a propósito)

- No implementa `/login` ni gestiona contraseñas.
- No genera ni firma Access Tokens.
- No tiene ningún `client_secret`.

Esas responsabilidades son 100% de Microsoft Entra ID (IDaaS), no de esta API.

## Repos relacionados

- Frontend: [biblioteca-frontend](https://github.com/Libro-Prestamo/biblioteca-frontend)
