# Contrato de eventos — Kafka

Define los tópicos, los eventos que circulan, quién los publica y quién los consume. Es el acuerdo entre servicios: si un payload cambia, se cambia aquí primero. Kafka complementa a RabbitMQ; el contrato de RabbitMQ está en [`eventos-rabbitmq.md`](eventos-rabbitmq.md).

## 1. Tópicos

| Tópico | Publica | Consumen (grupo) | Clave | Particiones | Réplicas | `cleanup.policy` | Retención |
|---|---|---|---|---|---|---|---|
| `prestamos.eventos` | ms-prestamo | ms-analitica (`analitica-prestamos`), ms-historial (`auditoria-prestamos`) | `libroId` | 3 | 3 | `delete` | 7 días (`604800000` ms) |
| `catalogo.eventos` | ms-catalogo | ms-analitica (`analitica-catalogo`) | `libroId` | 3 | 3 | `compact` | — |

Ambos con `min.insync.replicas=2`.

### Por qué estos valores

- **3 réplicas con 3 brokers:** cada broker guarda una copia de cada partición. Con `min.insync.replicas=2`, el tópico sigue aceptando escrituras si cae un broker.
- **3 particiones:** hasta 3 consumidores de un mismo grupo leen en paralelo.
- **Clave `libroId`:** los eventos de un mismo libro caen en la misma partición y se leen en orden. Entre particiones no hay orden garantizado.
- **`delete` en préstamos:** son eventos de hechos que dejan de ser útiles pasado un tiempo.
- **`compact` en catálogo:** se conserva el último evento por libro, que es el estado actual.
- **Sin tópico de errores:** los mensajes que fallan se reintentan y se registran en el log.

## 2. Grupos de consumo

| Grupo | Servicio | Tópico | Para qué |
|---|---|---|---|
| `analitica-prestamos` | ms-analitica | `prestamos.eventos` | Estadísticas de préstamos |
| `analitica-catalogo` | ms-analitica | `catalogo.eventos` | Estadísticas del catálogo |
| `auditoria-prestamos` | ms-historial | `prestamos.eventos` | Registro de auditoría |

## 3. Eventos

### Convenciones

- Cuerpo en **JSON UTF-8**. La clave del mensaje es `libroId` como texto.
- Fechas en **ISO-8601 UTC**.
- Todo evento trae `eventId` (UUID) y `tipo`. El consumidor usa `eventId` para ser **idempotente**.
- `usuario` es el claim `sub` del JWT, igual que en los eventos de RabbitMQ.
- Los campos se pueden **agregar**, pero no renombrar ni quitar sin acordarlo en el equipo. Los consumidores ignoran campos desconocidos.

### `PrestamoEventoDto` (`prestamos.eventos`)

`tipo`: `CREADO` o `DEVUELTO`.

```json
{
  "eventId": "a3f1c2e0-7b4d-4a1e-9c3f-1234567890ab",
  "tipo": "CREADO",
  "prestamoId": 101,
  "libroId": 5,
  "usuario": "bM3xQ7f2kLw9TnR8vYcZ1aPdE5uHgJ0sXoI4tKyWq6U",
  "fecha": "2026-11-20T15:30:00Z"
}
```

### `CatalogoEventoDto` (`catalogo.eventos`)

`tipo`: `CREADO`, `ACTUALIZADO`, `ELIMINADO` o `STOCK_AGOTADO`.

```json
{
  "eventId": "b7d2e9a1-3c5f-4e8b-a1d6-0987654321fe",
  "tipo": "STOCK_AGOTADO",
  "libroId": 5,
  "titulo": "Cien años de soledad",
  "stock": 0,
  "fecha": "2026-11-20T15:31:00Z"
}
```

## 4. Manejo de errores

1. El consumidor **valida** el payload (campos obligatorios y tipos conocidos).
2. Si falla el procesamiento, **reintenta** con espera entre intentos (número configurable).
3. Si el mensaje no se puede deserializar o agota los reintentos, se **registra en el log** con el tópico, la partición, el offset y el motivo, y se continúa con el siguiente.

## 5. Configuración del cliente

| Propiedad | Valor |
|---|---|
| `bootstrap-servers` | `${KAFKA_BOOTSTRAP_SERVERS}` (los 3 brokers) |
| Serializador de la clave | `StringSerializer` |
| Serializador del valor | JSON |
| `group-id` | el de la tabla de grupos |
| `auto-offset-reset` | `earliest` |