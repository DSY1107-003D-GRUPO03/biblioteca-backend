# Guía de desarrollo

Acuerdos técnicos para trabajar en el monorepo sin pisarnos.

## 1. Estructura de cada servicio

Paquete base: `cl.duoc.dsy1107.biblioteca_<servicio>` (por ejemplo `cl.duoc.dsy1107.biblioteca_prestamo`). `ms-catalogo` conserva por ahora el paquete `biblioteca_backend`; renombrarlo a `biblioteca_catalogo` es opcional.

Documentos de referencia: `contratos/api-rest.md` (endpoints) y `contratos/eventos-rabbitmq.md` (mensajería).

```
ms-<servicio>/
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/
├── .env.example               # plantilla de variables, sin valores secretos
└── src/main/java/.../<servicio>/
    ├── controller/            # REST, solo traducen HTTP ↔ servicio
    ├── service/               # lógica de negocio
    ├── repository/            # Spring Data JPA
    ├── model/                 # entidades
    ├── dto/                   # request / response
    ├── exception/             # excepciones + ApiExceptionHandler
    ├── security/              # SecurityConfig, validación de audience (copiar de ms-catalogo)
    └── messaging/
        ├── config/            # @Configuration: exchanges, colas, bindings, converter
        ├── event/             # records de los eventos (según eventos-rabbitmq.md)
        ├── publisher/         # productores (RabbitTemplate)
        └── listener/          # @RabbitListener, agrupados por dominio
```

La configuración de mensajería (`messaging/config`) **no contiene lógica de negocio**, y el negocio no conoce nombres de colas: los toma de `application.yml`.

## 2. Puertos

| Servicio | Puerto |
|---|---|
| ms-catalogo | 8080 |
| ms-prestamo | 8081 |
| ms-historial | 8082 |
| ms-rabbit-admin | 8083 |
| ms-notificaciones | (sin puerto HTTP) |
| RabbitMQ AMQP / consola | 5672 / 15672 (nodo 1), 5673 / 15673 (nodo 2) |

## 3. Variables de entorno

Cada servicio tiene su `.env` en su carpeta (no se versiona). Hay un `.env.example` con los nombres.

| Variable | ms-catalogo | ms-prestamo | ms-historial | ms-notificaciones | ms-rabbit-admin |
|---|:-:|:-:|:-:|:-:|:-:|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | ✔ | ✔ | ✔ | — | — |
| `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_JWK_SET_URI` | ✔ | ✔ | ✔ | — | ✔ |
| `RABBIT_ADDRESSES` (`host:5672,host:5673`) | ✔ | ✔ | ✔ | ✔ | ✔ |
| `RABBIT_USERNAME`, `RABBIT_PASSWORD` | ✔ | ✔ | ✔ | ✔ | ✔ |
| `CATALOGO_URL` (ej. `http://localhost:8080`) | — | ✔ | — | — | — |

Ejemplo de `.env.example` (ms-prestamo):

```dotenv
DB_URL=
DB_USERNAME=
DB_PASSWORD=
JWT_ISSUER=
JWT_AUDIENCE=
JWT_JWK_SET_URI=
RABBIT_ADDRESSES=localhost:5672
RABBIT_USERNAME=
RABBIT_PASSWORD=
CATALOGO_URL=http://localhost:8080
```

## 4. Base de datos

Un solo RDS (`biblioteca`). Para no pisarse, cada servicio usa **sus propias tablas**:

| Servicio | Tablas |
|---|---|
| ms-catalogo | `libro` |
| ms-prestamo | `prestamo` (guarda `libro_id` como número, sin llave foránea hacia `libro`) |
| ms-historial | `historial_entry` |

Un servicio **no lee las tablas de otro**: si necesita un dato, lo pide por REST o lo recibe en un evento. Por eso `Prestamo` deja de tener la relación `@ManyToOne Libro`.

## 5. Llamadas entre servicios

- `ms-prestamo` llama a `ms-catalogo` con `RestClient`, **reenviando el header `Authorization`** del usuario.
- Una llamada que falla por red devuelve `503` al cliente. Una respuesta `409` o `404` de catalogo se traduce tal cual.
- La URL del otro servicio viene de la variable `CATALOGO_URL`, nunca escrita en el código.

## 6. Git

- Rama principal: `main`. Cada persona trabaja en `feat/<servicio>-<tema>` y entra a `main` cuando **compila y arranca**.
- Antes de empezar a trabajar: `git pull`. Antes de subir: `git pull` otra vez.
- Cada servicio vive en su carpeta: no editar el servicio de otro sin avisar.
- Mensajes de commit en infinitivo y específicos: `feat(ms-prestamo): publica prestamo.creado`.
- **Nunca** subir `.env`, contraseñas, tokens ni `target/`. Revisar `git status` antes de cada commit.

## 7. Cómo probar un servicio

1. RabbitMQ arriba (`docker compose up -d`) y la consola en <http://localhost:15672>.
2. Arrancar el servicio: desde su carpeta, `./mvnw spring-boot:run` (en PowerShell, `.\mvnw`).
3. Obtener un token en el frontend (login con Entra ID) o con el flujo que se use para pruebas, y llamar la API con `Authorization: Bearer <token>`.
4. Verificar el efecto asíncrono en los logs del consumidor y en la consola de RabbitMQ (colas, mensajes, DLQ).

## 8. Definición de "terminado" para un servicio

- [ ] Compila (`./mvnw clean package`) y arranca sin errores.
- [ ] Sus endpoints cumplen `contratos/api-rest.md` (rutas, códigos y formato de error).
- [ ] Sus eventos cumplen `contratos/eventos-rabbitmq.md` (nombres, exchange, payload).
- [ ] Los nombres de colas y exchanges salen de `application.yml`.
- [ ] Los consumidores confirman con ACK manual y mandan a la DLQ los fallos.
- [ ] Tiene `.env.example` y su sección en el README.
- [ ] No hay secretos en el repo.
