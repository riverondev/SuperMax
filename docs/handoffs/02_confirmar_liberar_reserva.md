# Handoff 2 — CONFIRMAR_RESERVA: confirmar o liberar

Fecha: 16/10/2026. Destinatario: compañero responsable del servicio 3. Este documento permite iniciar el trabajo sin esperar a que termine RESERVAR_STOCK. No leer `entrega-01`.

## Objetivo y contexto

Implementar `CONFIRMAR_RESERVA` con acciones `CONFIRMAR` y `LIBERAR`. Hoy SuperMax solo atiende GET_CATALOGO. Las tablas nuevas están propuestas y aún no se aplicaron a la base local. Para el MVP, producto.stock es físico; el servicio 2 apartará unidades sin descontarlo.

Leer [estado y acuerdos compartidos](../Estado_actual.md), [modelo](../Base_de_datos.md), [SQL de reservas](../../db/002_reservas_propuesta.sql) y [contratos v3](../SuperMax_v3.md). Seguir los mismos nombres, estados, códigos de error, tipos y orden de bloqueos.

## Alcance y archivos propios

- Crear `src/main/java/fpuna/supermax/servicio/GestionReservaServicio.java` y `bd/GestionReservaDAO.java` bajo el mismo paquete base.
- Exponer `public JSONObject ejecutar(JSONObject solicitud) throws SQLException`. Devolver respuesta completa; propagar errores SQL después de rollback.
- DAO propio con métodos que reciben la Connection de la operación. No depender de ReservaStockDAO, de un DTO que el otro todavía esté escribiendo ni del código de creación.
- Pruebas y fixtures propios para reservas RESERVADA, vencida y finalizadas. Usar una base exclusiva de pruebas; no borrar ni alterar los productos locales de desarrollo.
- No modificar `TCPServerHilo`, `ProductoDAO`, el esquema compartido ni el POM sin coordinar: el responsable del handoff 1 integra esos archivos. Entregar el fragmento del case indicado abajo.

## Contrato

```json
{
  "tipo": "CONFIRMAR_RESERVA",
  "requestId": "req-confirmacion-01",
  "reservaId": "RES-ejemplo-01",
  "accion": "CONFIRMAR"
}
```

```json
{
  "ok": true,
  "requestId": "req-confirmacion-01",
  "reservaId": "RES-ejemplo-01",
  "pedidoExternoId": "PED-101",
  "estado": "CONFIRMADA",
  "timestamp": "2026-10-16T12:01:00-03:00"
}
```

Para liberar se usa el mismo tipo con `accion: "LIBERAR"`; la respuesta exitosa tiene `estado: "LIBERADA"`. Reflejar el requestId de esta solicitud, no el request_id guardado cuando se creó la reserva.

## Máquina de estados y reintentos

| Estado observado | CONFIRMAR | LIBERAR |
| --- | --- | --- |
| RESERVADA y vigente | Descontar físico y cambiar a CONFIRMADA. | Cambiar a LIBERADA sin modificar físico. |
| RESERVADA y vencida | Marcar EXPIRADA y devolver RESERVA_EXPIRADA. | Marcar EXPIRADA y devolver RESERVA_EXPIRADA. |
| CONFIRMADA | Éxito repetido, sin nuevo descuento. | ESTADO_INVALIDO. |
| LIBERADA | ESTADO_INVALIDO. | Éxito repetido, sin cambios. |
| EXPIRADA | RESERVA_EXPIRADA. | RESERVA_EXPIRADA. |

Comprobar primero los estados finales: una reserva CONFIRMADA no se convierte en EXPIRADA porque pasó su antigua fecha de vencimiento. La fecha se evalúa para reservas todavía RESERVADA. No deshacer compras confirmadas ni reabrir reservas. Una reservaId inexistente produce RESERVA_NO_EXISTE.

La repetición se resuelve por reservaId, acción y estado; el esquema no almacena cada requestId de este servicio. No sobrescribir reserva.request_id ni prometer un historial general de idempotencia que no está implementado.

## Flujo transaccional

1. Validar requestId y reservaId no vacíos y dentro de longitud (100 y 50); accion exactamente CONFIRMAR o LIBERAR. Tipos incorrectos, acción no admitida o longitudes inválidas producen SOLICITUD_INVALIDA.
2. Abrir Connection propia con READ_COMMITTED y autoCommit=false. Leer cabecera e items inicialmente sin FOR UPDATE para identificar productos; si no existe, devolver RESERVA_NO_EXISTE sin mutaciones.
3. Bloquear las filas de producto de los items, en orden ascendente de SKU, con SELECT ... FOR UPDATE. Después bloquear la fila de reserva con SELECT ... FOR UPDATE y releer estado. Este orden es compartido: no bloquear primero la reserva y luego productos. Los items son inmutables.
4. Tras esperar bloqueos, obtener la hora actual de PostgreSQL con clock_timestamp() y evaluar estado/plazo. Si aún está RESERVADA pero venció, actualizar EXPIRADA, hacer commit de esa transición y responder RESERVA_EXPIRADA. No ejecutar rollback automáticamente solo porque la respuesta de negocio tenga ok:false: este caso persiste el vencimiento.
5. CONFIRMAR vigente: descontar la cantidad de cada item de producto.stock y cambiar a CONFIRMADA en la misma transacción. Como defensa, cada UPDATE físico debe requerir stock >= cantidad y afectar exactamente una fila. Si falla alguna actualización, rollback completo y ERROR_BD por inconsistencia, sin descuentos parciales.
6. LIBERAR vigente: cambiar a LIBERADA. No sumar cantidades a producto.stock. Hacer commit; las consultas de disponibilidad ya dejan de contar esa reserva.
7. Acciones repetidas sobre el mismo estado final devuelven éxito sin mutaciones. Acciones incompatibles devuelven ESTADO_INVALIDO. Finalizar la transacción y liberar bloqueos en todos los caminos.
8. SQLException: rollback, cerrar recursos y propagar para que el dispatcher devuelva ERROR_BD. No capturar el fallo para devolver éxito o una lista vacía.

No agregar scheduler de expiración, reembolsos, preparación de pedidos ni comunicación con la BD de EntregaYa. El pedidoExternoId ya está guardado en la cabecera y se devuelve desde ahí.

## Cómo trabajar sin esperar al servicio 2

En una base de pruebas con producto y el SQL común aplicado, preparar una reserva y sus items mediante fixture SQL transaccional. Ejemplo conceptual: stock físico 10, reserva RES-prueba-01 por 3 unidades, creada_en actual y expira_en cinco minutos después. Crear otra vencida con creada_en diez minutos atrás y expira_en cinco minutos atrás, respetando el CHECK expira_en > creada_en.

Los fixtures deben incluir request_id único por reserva y SKUs que existan en esa base. La reserva activa no debe descontar stock al prepararse. Invocar ejecutar(JSONObject) directamente desde las pruebas hasta que el integrador agregue la ruta TCP. No hace falta un servicio 2 funcional para probar tu lógica y concurrencia.

## Pruebas de aceptación

| Caso | Resultado esperado |
| --- | --- |
| Confirmar 3 unidades sobre stock físico 10 | Físico 7 y estado CONFIRMADA. |
| Repetir confirmación, incluso en dos conexiones simultáneas | Físico 7; nunca 4. Ambas respuestas coherentes. |
| Liberar reserva vigente de 3 sobre stock 10 | Físico 10 y estado LIBERADA. |
| Repetir liberación | Éxito sin sumar stock. |
| Confirmar una liberada / liberar una confirmada | ESTADO_INVALIDO sin cambios. |
| Confirmar o liberar una reserva vencida | RESERVA_EXPIRADA; físico intacto; estado EXPIRADA persistido. |
| Confirmada con fecha antigua, repetir confirmar | Éxito repetido, sin expirar ni descontar de nuevo. |
| Reserva inexistente / entrada mal formada | Error controlado según acuerdos. |
| Reserva de varios productos | Todos los descuentos y estado cambian en una transacción. |
| Fallo SQL o stock inconsistente en un item | Rollback completo: no descontar solo parte. |
| Confirmar y liberar simultáneamente la misma reserva | Una única transición gana; sin descuento doble ni stock incrementado. |
| Una operación espera un bloqueo hasta después del vencimiento | No confirmar usando una hora anterior a la espera. |

Los casos contra RESERVAR_STOCK y GET_CATALOGO se prueban juntos al integrar. Estas pruebas son requisitos para el trabajo futuro, no resultados ya verificados.

## Entrega al integrador

Entregar servicio/DAO, pruebas, fixtures reproducibles y comandos/resultados de build y ejecución. Si una dependencia de pruebas requiere modificar POM, coordinarla con el integrador antes de introducirla. No alcanza con compilar: ejecutar pruebas de BD y concurrencia.

Fragmento que el integrador agregará al switch de TCPServerHilo, junto con el import correspondiente:

```java
case "CONFIRMAR_RESERVA":
    return new GestionReservaServicio().ejecutar(solicitud).toJSONString();
```

Indicar expresamente qué casos pasaron, cuáles faltan y si el esquema solo se aplicó en la base de pruebas. Mantener tus cambios fuera de los archivos asignados al otro desarrollador hasta la integración conjunta.
