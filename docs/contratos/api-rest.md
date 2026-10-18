# Contrato de API REST

Define las rutas, los cuerpos y los códigos de respuesta de cada servicio. Es el acuerdo entre el frontend y el backend, y entre los servicios que se llaman por REST. Si algo cambia, se cambia aquí primero.

## 1. Convenciones

| Tema | Regla |
|---|---|
| Formato | JSON UTF-8. Header `Content-Type: application/json` en las peticiones con cuerpo. |
| Autenticación | JWT de Microsoft Entra ID en `Authorization: Bearer <token>`. |
| Fechas | ISO-8601 sin zona, tal como las serializa Spring (`2026-10-22T15:30:00`). |
| Ids | Números enteros (`Long`). |
| Usuario | El campo `usuario` es el claim **`sub`** del JWT (identificador opaco de Entra ID, no el correo). |
| Errores | Todos los servicios responden con el mismo formato (ver abajo). |

### Formato de error

```json
{
  "status": 409,
  "error": "Conflict",
  "mensaje": "No hay stock disponible para el libro: Cien años de soledad"
}
```

Errores de validación (`400`) agregan la lista `errores`:

```json
{
  "status": 400,
  "error": "Bad Request",
  "mensaje": "La petición no es válida",
  "errores": [
    { "campo": "stock", "mensaje": "debe ser mayor o igual a 0" }
  ]
}
```

### Códigos comunes

| Código | Cuándo |
|---|---|
| 400 | El cuerpo no pasa la validación. |
| 401 | Falta el token o es inválido (firma, `iss`, `aud` o expiración). |
| 403 | El token es válido pero no tiene el scope o el rol necesario. |
| 404 | El recurso no existe. |
| 409 | Conflicto de negocio (sin stock, ISBN duplicado, préstamo ya devuelto). |
| 503 | Otro servicio del que depende no responde. |

### Permisos

| Rutas | Requisito |
|---|---|
| `/public/**` | Ninguno |
| `/api/**` | JWT válido |
| `/api/write/**` | Scope `prestamo.write` |
| `/api/admin/**` y todo `ms-rabbit-admin` | Rol `ADMIN` |

## 2. Enrutamiento en el API Gateway

El frontend siempre llama al API Gateway con las rutas de este documento.

| Prefijo de ruta | Servicio | Puerto local |
|---|---|---|
| `/public/libros`, `/api/libros/**`, `/api/write/libros/**`, `/api/admin/libros/**` | ms-catalogo | 8080 |
| `/api/prestamos`, `/api/write/prestamos/**` | ms-prestamo | 8081 |
| `/api/historial` | ms-historial | 8082 |
| `/rabbit/**` | ms-rabbit-admin | 8083 |

Para `/rabbit/**` el gateway **quita el prefijo `/rabbit`**: `POST /rabbit/queues` llega al servicio como `POST /queues`.

## 3. Modelos

### Libro

```json
{
  "id": 5,
  "titulo": "Cien años de soledad",
  "autor": "Gabriel García Márquez",
  "isbn": "9780307474728",
  "stock": 3,
  "disponible": true
}
```

`disponible` es `true` cuando `stock > 0`.

### Prestamo

```json
{
  "id": 101,
  "libroId": 5,
  "titulo": "Cien años de soledad",
  "usuario": "bM3xQ7f2kLw9TnR8vYcZ1aPdE5uHgJ0sXoI4tKyWq6U",
  "fechaPrestamo": "2026-10-22T15:30:00",
  "fechaDevolucion": null,
  "estado": "ACTIVO"
}
```

`estado` es `ACTIVO` o `DEVUELTO`. `fechaDevolucion` es `null` mientras el préstamo está activo.

### EntradaHistorial

```json
{
  "id": 1,
  "eventId": "a3f1c2e0-7b4d-4a1e-9c3f-1234567890ab",
  "tipo": "prestamo.creado",
  "usuario": "bM3xQ7f2kLw9TnR8vYcZ1aPdE5uHgJ0sXoI4tKyWq6U",
  "libroId": 5,
  "prestamoId": 101,
  "recibidoEn": "2026-10-22T15:30:02"
}
```

`usuario`, `libroId` y `prestamoId` pueden ser `null` según el tipo de evento.

---

## 4. ms-catalogo (puerto 8080)

### `GET /public/libros`

Lista el catálogo. **Público.**

| Código | Respuesta |
|---|---|
| 200 | Arreglo de `Libro` |

### `GET /api/libros/{id}`

Obtiene un libro. **JWT válido.**

| Código | Respuesta |
|---|---|
| 200 | `Libro` |
| 401 | Error |
| 404 | Error: el libro no existe |

### `POST /api/write/libros/{id}/stock/reservar`

**Uso interno:** lo llama `ms-prestamo` (reenviando el JWT del usuario). Descuenta una unidad. Si el stock queda en 0, publica el evento `stock.agotado`. **Scope `prestamo.write`.**

Sin cuerpo.

| Código | Respuesta |
|---|---|
| 200 | `Libro` con el stock ya descontado |
| 401 / 403 | Error |
| 404 | Error: el libro no existe |
| 409 | Error: no hay stock disponible |

### `POST /api/write/libros/{id}/stock/liberar`

**Uso interno:** lo llama `ms-prestamo` al registrar una devolución. Suma una unidad. **Scope `prestamo.write`.**

Sin cuerpo.

| Código | Respuesta |
|---|---|
| 200 | `Libro` con el stock ya sumado |
| 401 / 403 | Error |
| 404 | Error: el libro no existe |

### `POST /api/admin/libros`

Crea un libro. Publica `libro.creado`. **Rol `ADMIN`.**

Petición:

```json
{
  "titulo": "El túnel",
  "autor": "Ernesto Sabato",
  "isbn": "9788432217579",
  "stock": 4
}
```

| Campo | Regla |
|---|---|
| `titulo`, `autor` | Obligatorios, no vacíos, máximo 255 caracteres |
| `isbn` | Obligatorio, entre 10 y 17 caracteres, único |
| `stock` | Obligatorio, entero ≥ 0 |

| Código | Respuesta |
|---|---|
| 201 | `Libro` creado |
| 400 | Error de validación |
| 401 / 403 | Error |
| 409 | Error: ya existe un libro con ese ISBN |

### `DELETE /api/admin/libros/{id}`

Elimina un libro. Publica `libro.eliminado`. **Rol `ADMIN`.**

| Código | Respuesta |
|---|---|
| 204 | Sin cuerpo |
| 401 / 403 | Error |
| 404 | Error: el libro no existe |

---

## 5. ms-prestamo (puerto 8081)

### `GET /api/prestamos`

Lista los préstamos del usuario autenticado (solo los propios). **JWT válido.**

| Código | Respuesta |
|---|---|
| 200 | Arreglo de `Prestamo` |
| 401 | Error |

### `POST /api/write/prestamos`

Solicita un préstamo. **Scope `prestamo.write`.**

Flujo: (1) reserva el stock en `ms-catalogo` por REST, (2) guarda el préstamo, (3) publica `prestamo.creado`, (4) responde.

Petición:

```json
{ "libroId": 5 }
```

| Campo | Regla |
|---|---|
| `libroId` | Obligatorio, entero positivo |

| Código | Respuesta |
|---|---|
| 201 | `Prestamo` creado, con `estado: "ACTIVO"` |
| 400 | Error de validación |
| 401 / 403 | Error |
| 404 | Error: el libro no existe |
| 409 | Error: no hay stock disponible |
| 503 | `ms-catalogo` no responde |

### `POST /api/write/prestamos/{id}/devolucion`

Registra la devolución. Solo el dueño del préstamo puede hacerlo. Libera el stock en `ms-catalogo` y publica `prestamo.devuelto`. **Scope `prestamo.write`.**

Sin cuerpo.

| Código | Respuesta |
|---|---|
| 200 | `Prestamo` con `estado: "DEVUELTO"` y `fechaDevolucion` completada |
| 401 / 403 | Error. También `403` si el préstamo es de otro usuario |
| 404 | Error: el préstamo no existe |
| 409 | Error: el préstamo ya fue devuelto |
| 503 | `ms-catalogo` no responde |

---

## 6. ms-historial (puerto 8082)

### `GET /api/historial`

Lista los eventos registrados, del más reciente al más antiguo. **JWT válido.**

| Parámetro (query) | Obligatorio | Descripción |
|---|---|---|
| `tipo` | no | Filtra por tipo de evento, por ejemplo `prestamo.creado` |
| `usuario` | no | Filtra por usuario |

| Código | Respuesta |
|---|---|
| 200 | Arreglo de `EntradaHistorial` |
| 401 | Error |

---

## 7. ms-rabbit-admin (puerto 8083)

Todas las rutas requieren **rol `ADMIN`**. Por el API Gateway llevan el prefijo `/rabbit`. Directamente en el servicio, no.

Los nombres de colas y exchanges deben cumplir `^[a-zA-Z0-9._-]{1,100}$`.

### `POST /queues`

Crea una cola.

```json
{
  "name": "pedidos.queue",
  "durable": true,
  "quorum": true,
  "deadLetterExchange": "biblioteca.dlx",
  "deadLetterRoutingKey": "pedidos.queue.dlq"
}
```

| Campo | Obligatorio | Regla |
|---|---|---|
| `name` | sí | Cumple el patrón de nombres |
| `durable` | no | Por defecto `true` |
| `quorum` | no | Por defecto `true` (cola replicada entre nodos) |
| `deadLetterExchange` | no | Nombre de un exchange |
| `deadLetterRoutingKey` | no | Solo válido si hay `deadLetterExchange` |

| Código | Respuesta |
|---|---|
| 201 | La cola creada (mismo cuerpo) |
| 400 | Error de validación |
| 401 / 403 | Error |

### `DELETE /queues/{name}`

Elimina una cola.

| Código | Respuesta |
|---|---|
| 204 | Sin cuerpo |
| 401 / 403 | Error |
| 404 | Error: la cola no existe |

### `POST /exchanges`

Crea un exchange.

```json
{
  "name": "biblioteca.custom",
  "type": "topic",
  "durable": true
}
```

| Campo | Obligatorio | Regla |
|---|---|---|
| `name` | sí | Cumple el patrón de nombres |
| `type` | sí | `direct`, `topic` o `fanout` |
| `durable` | no | Por defecto `true` |

| Código | Respuesta |
|---|---|
| 201 | El exchange creado (mismo cuerpo) |
| 400 | Error de validación |
| 401 / 403 | Error |

### `DELETE /exchanges/{name}`

Elimina un exchange.

| Código | Respuesta |
|---|---|
| 204 | Sin cuerpo |
| 401 / 403 | Error |
| 404 | Error: el exchange no existe |

### `POST /bindings`

Crea un binding entre un exchange y una cola.

```json
{
  "exchange": "biblioteca.events",
  "queue": "pedidos.queue",
  "routingKey": "pedido.*"
}
```

| Campo | Obligatorio | Regla |
|---|---|---|
| `exchange` | sí | Debe existir |
| `queue` | sí | Debe existir |
| `routingKey` | sí | No vacío |

| Código | Respuesta |
|---|---|
| 201 | El binding creado (mismo cuerpo) |
| 400 | Error de validación |
| 401 / 403 | Error |
| 404 | Error: el exchange o la cola no existen |

### `DELETE /bindings`

Elimina un binding. El cuerpo identifica el binding, con los mismos campos que al crearlo.

| Código | Respuesta |
|---|---|
| 204 | Sin cuerpo |
| 400 | Error de validación |
| 401 / 403 | Error |
| 404 | Error: el exchange o la cola no existen |

Si el exchange y la cola existen pero el binding no, responde 204: la operación es idempotente (RabbitMQ no avisa si no había nada que borrar).

---

## 8. Cambios respecto al monolito

El monolito actual (`ms-catalogo` hoy) tiene todo junto. Al separar los servicios cambia esto:

| Tema | Hoy | Contrato |
|---|---|---|
| Estado del préstamo | Enum `ACTIVO`, `DEVULETO` (con typo) | `ACTIVO`, `DEVUELTO` |
| Respuesta de `Prestamo` | Devuelve la entidad con el objeto `libro` anidado | Devuelve `libroId` y `titulo` planos |
| Relación préstamo–libro | `@ManyToOne Libro` | `libroId` numérico, sin relación JPA (servicios separados) |
| Descuento de stock | `PrestamoService` modifica el `Libro` directamente | `ms-prestamo` llama a `POST .../stock/reservar` en `ms-catalogo` |
| Devolución | No existe | `POST /api/write/prestamos/{id}/devolucion` |
| Validación de entrada | Sin validación | `@Valid` en los `request`, formato de error `400` con `errores` |
| Creación de libro | Recibe la entidad `Libro` completa | Recibe un `NuevoLibro` (sin `id` ni `disponible`) |
