# Estado y acuerdos del MVP — 16/10/2026

Documento compartido para desarrollar los servicios 2 y 3 de SuperMax en paralelo. Distingue implementación existente y decisiones para el siguiente incremento. No se leyó `entrega-01`.

## Estado verificado

| Componente | Situación actual |
| --- | --- |
| SuperMax | Java 8, Maven, JDBC PostgreSQL 42.7.4, json-simple 1.1.1. |
| Arranque | `fpuna.supermax.Main` inicia `TCPMultiServer`; puerto 5001 o primer argumento. FQN coherente con el POM. |
| Transporte | Un hilo por conexión; un objeto JSON por línea en UTF-8; una solicitud y respuesta por socket. |
| GET_CATALOGO | Único servicio implementado. `TCPServerHilo` → `CatalogoServicio` → `ProductoDAO` → PostgreSQL. |
| Validación del catálogo | `sku` debe ser cadena; `""` consulta catálogo completo. Espacios no equivalen a cadena vacía. |
| Respuesta del catálogo | Lista de productos con precio y stock, `ok`, `requestId`, `timestamp`. Una lista vacía también devuelve `ok: true`. |
| Error de BD | `ProductoDAO` propaga `SQLException`; el servidor devuelve `ERROR_BD`. Ya no se confunde con una lista vacía. |
| EntregaYa | Cliente interactivo en `Main`, destino fijo 127.0.0.1:5001. Genera UUID, comprueba requestId, muestra errores `ok: false` y continúa. Los fallos de conexión/parseo todavía pueden terminar el programa. |
| PostgreSQL | Reconsultado el 16/10/2026 en modo de solo lectura: únicamente `public.producto`, con 3 filas. |
| Reservas | SQL propuesto disponible, todavía sin aplicar. No existen clases de servicio, DAOs ni rutas TCP para los servicios 2 y 3. |
| Verificación de este trabajo | Inspección de código, SQL y documentación, y lectura del esquema real. No se ejecutó una compilación ni pruebas de servicios en esta actualización documental. |

`producto` tiene `sku VARCHAR(50)` PK, `nombre VARCHAR(150)`, `precio BIGINT >= 0` y `stock INTEGER >= 0`; todos NOT NULL, sin defaults. Ver [modelo y propuesta SQL](Base_de_datos.md).

Hay cambios locales previos en `ProductoDAO.java` y en el proyecto EntregaYa. Cada desarrollador debe revisar su diff antes de trabajar y conservar cambios ajenos. No tomar documentos históricos del 02/10 como descripción del código actual si contradicen esta revisión.

## Acuerdos para los dos desarrolladores

Estas reglas definen el siguiente incremento; todavía no están implementadas.

1. Mantener el SQL de [002_reservas_propuesta.sql](../db/002_reservas_propuesta.sql): `reserva` y `reserva_item`. No agregar tablas de inventario, alertas ni pedidos externos.
2. `producto.stock` representa stock físico. Crear una reserva no lo descuenta. Confirmar descuenta una sola vez. Liberar o vencer no lo incrementa.
3. Disponibilidad = stock físico menos SUM de cantidades de reservas `RESERVADA` cuyo `expira_en` sea posterior al instante de evaluación. Reservas vencidas dejan de contar incluso si su estado almacenado sigue siendo `RESERVADA`.
4. Estados finales: `CONFIRMADA`, `LIBERADA`, `EXPIRADA`. No reabrir reservas ni permitir acciones opuestas sobre estados finales.
5. Vencimiento inicial: 300 segundos desde la creación. Persistir `expira_en`; no reiniciar el plazo por un reintento. Evaluar el vencimiento con la hora de PostgreSQL obtenida después de esperar bloqueos (`clock_timestamp()`), evitando basarse en la hora de inicio de una transacción que pudo esperar.
6. SKU sensible a mayúsculas/minúsculas, igual que el catálogo actual. Rechazar SKUs repetidos en `items` con `SOLICITUD_INVALIDA`, en lugar de agruparlos implícitamente. Cantidades: enteros positivos dentro del rango de INTEGER; rechazar decimales y desbordamientos.
7. `requestId` de creación es único y queda en `reserva.request_id`. Misma creación repetida con igual pedido y conjunto de items: devolver la reserva original con su estado actual y plazo restante, sin insertar ni renovar. Reutilizarlo con otro contenido: `REQUEST_ID_CONFLICTO`. El orden del array no altera la igualdad de los items.
8. El requestId de confirmar/liberar se refleja en la respuesta, pero no reemplaza `reserva.request_id`, que pertenece a la creación. Para el MVP, el servicio 3 evita dobles efectos mediante estado de la reserva y acción repetida; no se agrega una tabla general de respuestas.
9. Una operación completa usa una conexión JDBC propia, `autoCommit=false`, aislamiento `READ_COMMITTED`, commit al éxito y rollback al fallo. Los DAO de esa operación reciben la misma `Connection`; no abren una conexión por sentencia.
10. JSON: conservar `tipo`, `requestId`, `ok`, `codigo`, `mensaje` y los nombres camelCase de v3. SQL conserva snake_case. Cada servicio expone `public JSONObject ejecutar(JSONObject solicitud) throws SQLException`, valida sus campos y devuelve el objeto de respuesta; el dispatcher serializa y gestiona errores de infraestructura. Es una convención simple para este MVP, no una API ya existente.

## Orden único de bloqueos

El objetivo es que puedan programar en paralelo y que el servidor siga atendiendo clientes concurrentes. Operaciones sobre filas compartidas sí pueden esperar brevemente en PostgreSQL.

**Regla: productos en orden de SKU → reserva existente.** No usar un bloqueo global Java, de tabla o de toda la aplicación.

- Servicio 2: bloquear filas de `producto` con `SELECT ... FOR UPDATE`, en orden ascendente de SKU; después consultar disponibilidad en una nueva sentencia bajo READ COMMITTED; insertar cabecera e items en la misma transacción. No bloquear reservas ajenas para sumar sus cantidades.
- Servicio 3: leer primero reserva e items sin bloquear para identificar SKUs (los items son inmutables); bloquear los productos en el mismo orden; después bloquear la cabecera con `SELECT ... FOR UPDATE`. Releer estado y vencimiento después de adquirir los bloqueos antes de decidir. No eliminar ni editar los items existentes en ninguno de los servicios.
- Para confirmar/liberar la misma reserva concurrentemente, la primera transición válida gana; la segunda lee el estado final y responde según las reglas de repetición. Dos confirmaciones solo descuentan una vez.
- Cualquier limpieza futura que escriba estados deberá respetar el mismo orden. No implementar un proceso periódico para este MVP; la consulta por fecha ya libera disponibilidad al vencer.
- Un bloqueo no reemplaza la transacción. Ante deadlock/timeout de bloqueo, rollback y respuesta de fallo controlada; el cliente puede reintentar. No ocultar el error como falta de stock.

## Reparto y propiedad de archivos

| Responsable | Archivos propios propuestos | Responsabilidad |
| --- | --- | --- |
| Handoff 1 | `servicio/ReservaStockServicio.java`, `bd/ReservaStockDAO.java` | Crear reservas e items, disponibilidad, repetición por requestId. |
| Handoff 2 | `servicio/GestionReservaServicio.java`, `bd/GestionReservaDAO.java` | Confirmar/liberar, estados finales, vencimiento y descuento único. |
| Handoff 1: integración | `server/TCPServerHilo.java`, `bd/ProductoDAO.java`, `servicio/CatalogoServicio.java`, SQL, POM si hacen falta dependencias de pruebas | Integrar ambos cases TCP, catálogo disponible, esquema y prueba conjunta. |
| Ambos | Pruebas y fixtures con nombres propios | Cada uno prueba su servicio sin depender de que el otro haya terminado. |

Los paths de clases son relativos a `src/main/java/fpuna/supermax/`. Los nombres son una propuesta de reparto. Evitar crear un `ReservaDAO.java` compartido o un DTO compartido que ambos editen simultáneamente. Cada implementación puede usar clases auxiliares propias si las necesita.

Handoff 2 entrega el servicio y el fragmento de integración en su nota de entrega, sin modificar `TCPServerHilo`. Handoff 1 agrega primero su case y, después de incorporar la clase del compañero, agrega el segundo. Así ninguna rama necesita importar una clase que todavía no existe. `Main`, los servicios propios de EntregaYa, UDP y preparación de pedidos quedan fuera de alcance.

## Errores y estados de respuesta

Estos códigos complementan v3 para que ambos servicios coincidan; son decisiones de implementación del MVP, no comportamiento actual.

| Código | Uso |
| --- | --- |
| CAMPO_REQUERIDO | Falta un identificador obligatorio o está vacío. |
| SOLICITUD_INVALIDA | Tipo/longitud inválidos, items vacíos, SKU repetido, cantidad no entera/positiva o acción distinta de CONFIRMAR/LIBERAR. |
| PRODUCTO_NO_EXISTE | Algún SKU no existe. |
| SIN_STOCK | Disponibilidad insuficiente para al menos un item; no crear nada. |
| REQUEST_ID_CONFLICTO | El requestId de creación ya corresponde a otro contenido. |
| RESERVA_NO_EXISTE | No existe reservaId. |
| RESERVA_EXPIRADA | La reserva sigue reservada pero venció, o ya está EXPIRADA. |
| ESTADO_INVALIDO | Acción incompatible con el estado final existente. |
| ERROR_BD | Fallo SQL; no devolver datos parciales ni detalles internos al cliente. |

Éxitos y errores incluyen el requestId de la solicitud actual si pudo leerse. Respuestas de error usan `ok: false`, `codigo`, `mensaje`; se conserva el formato de error actual del servidor.

## Integración del catálogo y cierre conjunto

Handoff 1 ajusta `ProductoDAO` para calcular disponibilidad mediante una sola consulta con agregación y LEFT JOIN, tanto para catálogo completo como filtrado. Evitar una consulta por producto. Mantener `stock` como nombre JSON de las unidades disponibles; catálogo vacío sigue siendo éxito y un producto sin unidades puede devolverse con stock 0.

Comprobación conjunta mínima: 10 unidades físicas → reservar 3 → catálogo 7 y físico 10 → confirmar → catálogo 7 y físico 7. En escenarios separados, liberar o vencer una reserva de 3 devuelve disponibilidad 10 y mantiene físico 10. Agregar concurrencia y reintentos conforme a los handoffs.

Cada uno puede trabajar en su rama del repositorio SuperMax; EntregaYa es otro repositorio. Aplicar el SQL una sola vez por base de desarrollo y usar una base exclusiva para pruebas que creen o borren datos. No borrar la tabla local producto ni ejecutar fixtures destructivos sobre `supermax`.

## Handoffs

- [1: Reservar stock e integración](handoffs/01_reservar_stock.md).
- [2: Confirmar o liberar reserva](handoffs/02_confirmar_liberar_reserva.md).
- [Contratos JSON v3](SuperMax_v3.md).
