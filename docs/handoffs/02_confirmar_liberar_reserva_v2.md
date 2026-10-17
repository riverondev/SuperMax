# Handoff 2 v2 — Confirmar o liberar una reserva

Referencia de mensajes y ejemplos: [Contratos reducidos v2](../Contratos_reducidos_v2.md).

Fecha: 16/10/2026. Implementación académica del tercer servicio de SuperMax.
Este documento conserva el encargo v2. Al 17/10/2026 gestión y su case TCP están integrados; ver [estado actual](../Estado_actual.md) y [nota de entrega](../entrega_handoff_02_v2.md) para la evidencia y pruebas pendientes. Los pasos siguientes describen el encargo original.
No leer `entrega-01`. Ver [handoff 1 v2](01_reservar_stock_v2.md) para el esquema e integración.

## Acuerdo reducido

Esta v2 reemplaza el handoff 2 original para este incremento. Los documentos anteriores conservan un alcance más amplio que se actualizará al integrar.
Sin vencimiento, EXPIRADA, timestamps nuevos, scheduler ni persistencia de requestId.
requestId solo se refleja en la respuesta; no se compara con el de creación.
Estados válidos: RESERVADA, CONFIRMADA y LIBERADA.
producto.stock es físico. Reservar no lo cambia; confirmar descuenta una sola vez; liberar no lo incrementa.
Disponibilidad = físico menos cantidades de reservas RESERVADA. Una reserva permanece hasta confirmarse o liberarse.

## Contrato

Entrada:
```json
{"tipo":"CONFIRMAR_RESERVA","requestId":"req-02","reservaId":1,"accion":"CONFIRMAR"}
```
Éxito:
```json
{"ok":true,"requestId":"req-02","reservaId":1,"estado":"CONFIRMADA"}
```
Liberación: mismo tipo con accion='LIBERAR'; devuelve estado='LIBERADA'.
Error:
```json
{"ok":false,"requestId":"req-02","codigo":"RESERVA_NO_EXISTE","mensaje":"No existe la reserva indicada"}
```
reservaId es un número entero positivo, no una cadena RES-uuid. No devolver pedidoExternoId ni timestamp.
Errores: CAMPO_REQUERIDO, SOLICITUD_INVALIDA, RESERVA_NO_EXISTE, ESTADO_INVALIDO y ERROR_BD.
Incluir requestId cuando pudo leerse como cadena; en otro caso null.

## Modelo disponible

- reserva: reserva_id SERIAL PK, pedido_externo_id VARCHAR(50) obligatorio y estado obligatorio restringido a los tres estados.
- reserva_item: reserva_id INTEGER FK, sku VARCHAR(50) FK y cantidad INTEGER positiva; PK (reserva_id, sku).
- producto conserva su esquema y su stock físico. Los ítems de una reserva no se editan ni eliminan.
- Entidades compartidas: Reserva(Integer reservaId, String pedidoExternoId, String estado, List<ReservaItem> items) y ReservaItem(Integer reservaId, String sku, Integer cantidad).
- No hay request_id ni columnas de fecha. El SQL antiguo de reservas no corresponde a esta versión.

El integrador prepara el esquema común y las entidades. No migrar datos, recrear tablas ni aplicar SQL antiguo por cuenta propia; usar el SQL definitivo en una base de pruebas independiente.

## Responsabilidades y tamaño

Archivos propios: servicio/GestionReservaServicio.java y bd/GestionReservaDAO.java.
Priorizar archivos breves y legibles; unas 100 líneas son una referencia, no un límite obligatorio.
No comprimir instrucciones ni crear una clase por método. Separar responsabilidades cuando mejore la comprensión, no para cumplir un número de líneas.
Se usan dos DAO de operación para evitar un DAO compartido grande y conflictos entre desarrolladores. No crear además un DAO por entidad.
Servicio: public JSONObject ejecutar(JSONObject solicitud) throws SQLException.
DAO: public Reserva gestionarReserva(int reservaId, String accion) throws SQLException.
CONFIRMAR y LIBERAR comparten lectura, bloqueo y validación de estado dentro de esa operación.
El DAO abre/cierra la conexión y controla commit/rollback; sus consultas auxiliares usan esa misma conexión. El servicio valida JSON y construye respuestas.
Usar la ValidacionException pequeña acordada con el integrador para errores de negocio; capturarla en el servicio. Propagar SQLException para ERROR_BD.
No modificar TCPServerHilo, ProductoDAO, entidades ni SQL mientras el integrador trabaja en ellos; entregar el fragmento de integración.

## Estados

| Estado actual | CONFIRMAR | LIBERAR |
| --- | --- | --- |
| RESERVADA | Descontar físico y pasar a CONFIRMADA. | Pasar a LIBERADA, físico intacto. |
| CONFIRMADA | Éxito sin nuevo descuento. | ESTADO_INVALIDO. |
| LIBERADA | ESTADO_INVALIDO. | Éxito sin cambios. |

La repetición se resuelve únicamente por estado y acción. No agregar historial ni tabla de respuestas.

## Implementación paso a paso

1. Validar requestId como cadena no vacía de hasta 100 caracteres; reservaId como entero positivo hasta Integer.MAX_VALUE; accion exactamente CONFIRMAR o LIBERAR. Con json-simple, comprobar Long y rango antes de convertir a int; no truncar decimales.
2. Abrir Connection dentro del DAO, configurar READ_COMMITTED y autoCommit=false. Consultar reserva e ítems sin bloquear para identificar SKU; si no existe, terminar con RESERVA_NO_EXISTE.
3. Bloquear productos con SELECT ... FOR UPDATE en orden de SKU. Después bloquear la cabecera de reserva con SELECT ... FOR UPDATE y releer su estado. No tomar primero el bloqueo de reserva.
4. Aplicar la tabla de estados usando el estado releído. Para repetición exitosa, cerrar la transacción sin descuentos; para acción incompatible, rollback y ESTADO_INVALIDO.
5. CONFIRMAR: por cada ítem ejecutar UPDATE producto SET stock=stock-? WHERE sku=? AND stock>=?. Comprobar una fila afectada por ítem; si alguno falla, lanzar SQLException y revertir todo por inconsistencia.
6. Actualizar estado a CONFIRMADA en la misma transacción que los descuentos. LIBERAR solo actualiza estado a LIBERADA. Comprobar la actualización de la cabecera y hacer commit.
7. Devolver la reserva con el estado resultante; el servicio arma JSON. Ante error SQL o de negocio, rollback y cierre de recursos. Si rollback falla, conservar también el error original.

Los bloqueos permanecen hasta terminar la transacción. Sincronizar por filas de PostgreSQL, no con un bloqueo global Java. Una reserva válida siempre tiene ítems; si los datos están incompletos, tratarlo como inconsistencia de BD.

## Trabajo independiente y pruebas

Preparar fixtures en la base de pruebas: producto con físico 10, cabecera RESERVADA y un ítem de cantidad 3. Usar el ID devuelto por PostgreSQL; no asumir que será 1. La preparación del fixture también debe ser transaccional.
Invocar el servicio desde una prueba o cliente separado; no depender de que RESERVAR_STOCK ya esté listo.
- Confirmar: físico 7 y estado CONFIRMADA; repetir conserva físico 7.
- Liberar en otro fixture: físico 10 y estado LIBERADA; repetir no suma unidades.
- Confirmar liberada o liberar confirmada: ESTADO_INVALIDO sin cambios.
- Reserva inexistente o entrada inválida: error controlado.
- Varios productos: todos los descuentos y el estado se guardan juntos.
- Fallo en un descuento: rollback también de los descuentos anteriores.
- Dos confirmaciones concurrentes: un solo descuento.
- Confirmar y liberar concurrentemente: gana una transición; la otra obtiene ESTADO_INVALIDO.

## Entrega al integrador

Entregar servicio/DAO, pruebas breves, fixtures y resultados reales de mvn package y ejecución.
Fragmento TCP (agregar su import al integrar):
```java
case "CONFIRMAR_RESERVA":
    return new GestionReservaServicio().ejecutar(solicitud).toJSONString();
```
Comprobar juntos catálogo: confirmar conserva disponibilidad 7 y deja físico 7; liberar en otro escenario devuelve disponibilidad 10 y mantiene físico 10.
No incluir pagos, reembolsos, preparación, UDP ni modificaciones a EntregaYa. Comunicar al consumidor el contrato numérico de reservaId.
Revisar claridad y responsabilidades, y señalar las pruebas pendientes antes de marcar el handoff terminado. No imponer un límite de líneas.
