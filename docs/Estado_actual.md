# Estado de SuperMax — 17/10/2026

Referencia vigente: [contratos reducidos v2](Contratos_reducidos_v2.md). Este documento describe el código integrado; los handoffs son encargos de trabajo y los diseños originales se conservan como antecedentes.

## Implementación actual

| Componente | Estado |
| --- | --- |
| Arranque | Main inicia TCPMultiServer; puerto 5001 o primer argumento. |
| Transporte | Un hilo por conexión, UTF-8, un objeto JSON por línea y una respuesta por socket. |
| GET_CATALOGO | Integrado: catálogo completo con sku vacío o filtrado; devuelve disponibilidad, precio y timestamp. Lista vacía es éxito. |
| RESERVAR_STOCK | Integrado: valida pedido e ítems, bloquea productos, consulta disponibilidad e inserta cabecera y detalles juntos. |
| CONFIRMAR_RESERVA | Integrado: CONFIRMAR descuenta una sola vez; LIBERAR cambia el estado sin sumar stock físico. |
| Persistencia | ProductoDAO, ReservaStockDAO, GestionReservaDAO y su auxiliar GestionReservaConsultas. |
| Entidades | Reserva con ID Integer, pedido externo, estado e ítems; ReservaItem con ID Integer, SKU y cantidad. Sin fechas ni requestId persistido. |
| Errores | ValidacionException representa errores de validación y rechazos de negocio; los servicios la convierten en respuestas JSON. En reservas, texto, items y DAO comparten un único try/catch. SQLException llega al dispatcher como ERROR_BD sin detalles internos. |
| Validación TCP | requestId se valida antes de tipo y también en los tres servicios: cadena no vacía/blanca de hasta 100 caracteres. SKU ausente/null o vacío/blanco dentro de items produce CAMPO_REQUERIDO; tipo incorrecto o longitud excesiva produce SOLICITUD_INVALIDA. Catálogo permite sku vacío y rechaza sku ausente/null con SOLICITUD_INVALIDA. |
| Respuestas | Los tres servicios incluyen timestamp en respuestas exitosas, no en errores. Representa la hora de respuesta, no un vencimiento de reserva. |
| Cliente de pruebas | ClienteJson permite enviar texto JSON por línea, muestra respuestas y continúa ante fallos de conexión sin reintentar. |
| Fuera de alcance | GET_PREPARACION, ALERTA_STOCK/UDP, pagos y cambios en EntregaYa. |

## Reglas acordadas e implementadas

- producto.stock es físico; el stock del catálogo es disponibilidad.
- Disponibilidad = físico menos cantidades de reservas RESERVADA.
- Crear no descuenta físico. Confirmar cambia a CONFIRMADA y descuenta dentro de la misma transacción. Liberar cambia a LIBERADA y no modifica físico.
- Repetir la acción compatible sobre un estado final es éxito sin cambios; la acción opuesta produce ESTADO_INVALIDO.
- No hay vencimiento, EXPIRADA, UUID de reserva ni deduplicación de creaciones. Reenviar RESERVAR_STOCK puede crear otra reserva.
- Cada DAO de escritura abre una conexión propia y controla READ_COMMITTED, autoCommit=false, commit y rollback. Sus consultas usan esa misma conexión.
- Orden de bloqueo: productos por SKU y luego cabecera de reserva existente. Los ítems persistidos son inmutables.
- El catálogo consulta disponibilidad con una sentencia agregada por llamada.
- Se priorizan claridad y métodos breves. Las 100 líneas fueron una referencia de sencillez, no un límite obligatorio ni un motivo para dividir clases artificialmente.

## Base de datos y preparación

Modelo vigente: [Base_de_datos.md](Base_de_datos.md).
La consulta de solo lectura realizada durante esta sesión verificó las columnas v2 de producto, reserva y reserva_item en la base local. Esa consulta no acredita que se hayan verificado todas las restricciones, ni fija cantidades actuales de filas o stocks.

En una base nueva usar 001_producto.sql y 002_reservas_v2.sql; seed opcional 003_seed_productos_v2.sql. No repetir DDL sobre tablas existentes ni aplicar el antiguo 002_reservas_propuesta.sql como migración. No se ejecutó DDL ni se reiniciaron datos durante esta actualización documental.

## Evidencia y pruebas pendientes

| Verificación | Evidencia disponible |
| --- | --- |
| Compilación integrada | mvn -o package terminó con BUILD SUCCESS en esta sesión. |
| Validación del servicio 3 | GestionReservaValidacion se ejecutó con resultado OK después del merge. No accede a BD. |
| Integración del handoff 2 | El compañero informó resultados OK de confirmación, liberación, rollback y concurrencia en su base de pruebas; ver su nota de entrega. No se repitieron contra BD después del merge en esta revisión. |
| Pruebas manuales TCP | El usuario informó que pudo probar los JSON con el cliente. No hay un registro detallado de todas las solicitudes y resultados. |
| Cierre conjunto | Falta registrar creación y catálogo junto con gestión, creación concurrente sin sobreventa y rollback de creación. |

Las clases de comprobación tienen main y no las descubre automáticamente Maven. Instrucciones reproducibles: [nota del handoff 2](entrega_handoff_02_v2.md) y [README](../README.md).
Los fixtures de integración vacían tablas exclusivamente en supermax_gestion_test; no usarlos en desarrollo.

## Reparto integrado y referencias

- [Handoff 1 v2](handoffs/01_reservar_stock_v2.md): creación, catálogo y dispatcher.
- [Handoff 2 v2](handoffs/02_confirmar_liberar_reserva_v2.md): gestión y consultas auxiliares.
- [Cliente JSON](Cliente_json.md): arranque y ejemplos con seed.
- [Diseño original v3](SuperMax_v3.md): especificación histórica de cinco servicios.
- Los handoffs originales con vencimiento y requestId persistido están reemplazados por v2.

No marcar la verificación conjunta como completa solo por compilar o por conservar resultados anteriores al merge.
