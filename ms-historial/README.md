# ms-historial

Microservicio que guarda el historial de eventos de la biblioteca. Recibe los mensajes desde RabbitMQ, los almacena en MySQL (RDS) y permite consultar los registros por HTTP.

## Tecnologías

- Java 21 y Spring Boot 4.1.1.
- Spring AMQP y RabbitMQ para consumir eventos.
- Spring Data JPA y MySQL para guardar el historial en la tabla `historial_entry`.
- Spring Security y JWT de Microsoft Entra ID para proteger la API.
- H2 se mantiene **solo como dependencia de pruebas** en el `pom.xml`, no como base de datos de ejecución.

## ¿Cómo funciona?

1. `ms-prestamo` publica eventos `prestamo.creado` y `prestamo.devuelto`. `ms-catalogo` publica `libro.creado` y `libro.eliminado`.
2. RabbitMQ envía esos eventos a `historial.queue`, enlazada al exchange `biblioteca.events` con `prestamo.*` y `libro.*`.
3. `HistorialListener` recibe el mensaje. Si es inválido o no se puede procesar luego de los reintentos, se envía a `historial.queue.dlq`.
4. `HistorialService` evita guardar dos veces el mismo `eventId` y almacena los datos en MySQL.
5. Un cliente autorizado puede consultar el historial por `GET /api/historial`.

## Base de datos

Se usa la misma instancia/base MySQL RDS `biblioteca` del proyecto. Cada microservicio usa sus propias tablas; el historial trabaja solo con `historial_entry`.

En `application.properties` se leen `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`. El ajuste `spring.jpa.hibernate.ddl-auto=update` mantiene el mismo criterio de los otros microservicios del repositorio para el entorno académico.

## Configuración

Desde la carpeta `ms-historial`, copia `.env.example` a `.env` y completa los valores reales (sin subir `.env` al repositorio):

```properties
DB_URL=jdbc:mysql://HOST_RDS:3306/biblioteca
DB_USERNAME=TU_USUARIO
DB_PASSWORD=TU_CLAVE
JWT_ISSUER=https://login.microsoftonline.com/TENANT_ID/v2.0
JWT_AUDIENCE=api://ID_DE_LA_API
JWT_JWK_SET_URI=https://login.microsoftonline.com/TENANT_ID/discovery/v2.0/keys
RABBIT_ADDRESSES=localhost:5672,localhost:5673
RABBIT_USERNAME=TU_USUARIO_RABBIT
RABBIT_PASSWORD=TU_CLAVE_RABBIT
```

Los valores de JWT deben coincidir con los utilizados en `ms-prestamo`. Para RDS, se necesita conectividad de red y permisos de acceso desde donde se ejecuta el microservicio. Si estás usando solo un nodo local de RabbitMQ, configura `RABBIT_ADDRESSES=localhost:5672`.

## Ejecutar

Primero inicia RabbitMQ y los servicios productores según `docs/guia-de-desarrollo.md`. Luego abre PowerShell dentro de `ms-historial` y ejecuta:

```powershell
.\mvnw.cmd spring-boot:run
```

## API REST

Solo está habilitada la consulta autenticada con **JWT válido**:

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/historial` | Lista todos los eventos, del más reciente al más antiguo. |
| GET | `/api/historial?tipo=prestamo.creado` | Filtra por tipo de evento. |
| GET | `/api/historial?usuario=SUB_DEL_USUARIO` | Filtra por el claim `sub` del usuario. |
| GET | `/api/historial?tipo=prestamo.creado&usuario=SUB_DEL_USUARIO` | Combina ambos filtros. |

La respuesta usa los campos `id`, `eventId`, `tipo`, `usuario`, `libroId`, `prestamoId` y `recibidoEn`, de acuerdo con `docs/contratos/api-rest.md`. El orden es descendente según la fecha de recepción; en empates, según el ID.

Una petición sin token debe devolver **401 Unauthorized**. La lectura no exige un scope especial: basta un JWT validado con firma, expiración, issuer y audience. El campo `usuario` corresponde al `sub` del JWT, no necesariamente al correo electrónico.

### Pruebas desde PowerShell

```powershell
# Sin token: se espera HTTP 401
curl.exe -i http://localhost:8082/api/historial

# Con token JWT válido para la API
$token = "TU_TOKEN_VALIDO"
Invoke-RestMethod -Uri 'http://localhost:8082/api/historial?tipo=prestamo.creado' `
    -Headers @{ Authorization = "Bearer $token" }
```

Al publicar un evento de préstamo o de libro, el listener debe mostrar `Historial registrado` y la consulta debe devolverlo. Para comprobar el orden, publica más de un evento y revisa que el registro más reciente aparezca primero.

## Archivos principales

```text
config/RabbitConfig.java            # Exchange, colas, bindings y DLQ
controller/HistorialController.java  # GET /api/historial y filtros
service/HistorialService.java        # Registro, duplicados y búsquedas
repository/HistorialRepository.java  # Consultas ordenadas y filtradas
model/HistorialEntry.java           # Entidad JPA, tabla historial_entry
dto/EntradaHistorial.java           # Respuesta de la API
listener/HistorialListener.java     # Consumer RabbitMQ, ACK/NACK y reintentos
security/SecurityConfig.java        # Seguridad y validación JWT
security/AudienceValidator.java     # Validación de audience
```

> Nota: el `README.md` documenta el funcionamiento del código. No sustituye las pruebas reales contra MySQL/RDS, Entra ID y RabbitMQ.
