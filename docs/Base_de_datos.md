# Base de datos de SuperMax — modelo reducido v2

Actualización: 17/10/2026. Modelo utilizado por los tres servicios integrados. Referencia: [contratos v2](Contratos_reducidos_v2.md).

## Esquema vigente

| Tabla | Columna | Tipo y reglas del SQL |
| --- | --- | --- |
| producto | sku | VARCHAR(50), PK, NOT NULL. |
| producto | nombre | VARCHAR(150), NOT NULL. |
| producto | precio | BIGINT, NOT NULL, CHECK >= 0. |
| producto | stock | INTEGER, NOT NULL, CHECK >= 0; existencias físicas. |
| reserva | reserva_id | SERIAL PRIMARY KEY; generado por PostgreSQL. |
| reserva | pedido_externo_id | VARCHAR(50), NOT NULL; no es FK ni UNIQUE. |
| reserva | estado | VARCHAR(20), NOT NULL, DEFAULT RESERVADA; CHECK permite RESERVADA, CONFIRMADA y LIBERADA. |
| reserva_item | reserva_id | INTEGER, NOT NULL, FK a reserva. |
| reserva_item | sku | VARCHAR(50), NOT NULL, FK a producto. |
| reserva_item | cantidad | INTEGER, NOT NULL, CHECK > 0. |

reserva_item tiene PK compuesta (reserva_id, sku) e índice adicional por sku. Las FK no tienen borrado en cascada. Una reserva tiene varios ítems; un SKU aparece una vez por reserva. El servicio rechaza SKU repetidos, cantidades inválidas y listas vacías antes de escribir.

La FK no garantiza por sí sola que una cabecera tenga detalles: la operación crea cabecera e ítems en una sola transacción. Los ítems no se modifican después de crear la reserva.

No hay request_id, creada_en ni expira_en. requestId pertenece al mensaje y solo se refleja en la respuesta. pedido_externo_id referencia un pedido de otra organización sin vincular bases mediante una FK.

## Correspondencia con Java

- Reserva: Integer reservaId, String pedidoExternoId, String estado y List<ReservaItem> items.
- ReservaItem: Integer reservaId, String sku e Integer cantidad.
- Producto conserva String sku/nombre, Long precio e Integer stock.
- El ID nace en INSERT ... RETURNING reserva_id y se utiliza para todos los detalles. No se genera UUID.

## Stock y estados

Disponible = producto.stock menos SUM de cantidades asociadas a reservas RESERVADA. COALESCE convierte una suma ausente en cero.

| Operación | Estado final | Físico | Disponibilidad |
| --- | --- | --- | --- |
| Reservar 3 con físico 10 | RESERVADA | 10 | 7 |
| Confirmar esa reserva | CONFIRMADA | 7 | 7 |
| Repetir confirmación | CONFIRMADA | 7 | 7 |
| Liberar 3 en un escenario independiente con físico 10 | LIBERADA | 10 | 10 |

No existe vencimiento automático. Una reserva abandonada sigue reteniendo unidades hasta liberarse. Reenviar una creación puede generar otra reserva; no hay deduplicación por requestId ni pedido externo.

## Transacciones

ReservaStockDAO y GestionReservaDAO abren y cierran su conexión; coordinan READ_COMMITTED, autoCommit=false, commit y rollback. Los métodos auxiliares reciben esa misma conexión.

1. Para crear: bloquear todos los productos en orden de SKU, después consultar disponibilidad en nuevas sentencias; insertar cabecera e ítems solo si alcanza para todos.
2. Para gestionar: leer ítems, bloquear productos en el mismo orden y luego bloquear/releer cabecera. Confirmar descuenta físico y cambia estado en la misma transacción; liberar solo cambia estado.
3. Repetir una acción compatible no produce nuevos efectos. Una acción opuesta sobre estado final es ESTADO_INVALIDO.
4. Ante fallos, revertir la operación completa. Las restricciones SQL complementan las comprobaciones; no reemplazan los bloqueos.
5. ProductoDAO calcula disponibilidad con una consulta agregada por llamada para obtener una lectura consistente del catálogo.

## Scripts y seed

En una base nueva, ejecutar en orden:

1. [001_producto.sql](../db/001_producto.sql).
2. [002_reservas_v2.sql](../db/002_reservas_v2.sql).
3. [003_seed_productos_v2.sql](../db/003_seed_productos_v2.sql), opcional.

No repetir los CREATE TABLE sobre tablas existentes. Los scripts son de inicialización, no una migración desde el esquema anterior. Si ya existe otra estructura, inspeccionarla y preparar una migración que preserve datos.

El seed agrega cuatro productos DEMO-* mediante ON CONFLICT DO NOTHING. No sobrescribe precios, nombres ni stocks existentes y no reinicia datos. Su SELECT final muestra stock físico; GET_CATALOGO muestra disponibilidad.

## Estado de verificación y antecedente

Durante esta sesión se consultaron en modo de solo lectura las columnas de la base local supermax y se verificó que coinciden con v2, incluido el ID entero con secuencia y el estado por defecto. No se repitió aquí una inspección completa de restricciones ni se publican conteos de filas que puedan quedar obsoletos.

El diseño anterior incluía ID de texto, request_id único, fechas y EXPIRADA. Su [SQL propuesto original](../db/002_reservas_propuesta.sql) se conserva como antecedente y no debe aplicarse para v2. Las antiguas revisiones que encontraron únicamente producto describían una etapa anterior, no el esquema actual.

Para pruebas con TRUNCATE, usar solo supermax_gestion_test. Ver [estado y evidencia](Estado_actual.md) y [reproducción de pruebas](entrega_handoff_02_v2.md). Esta edición documental no ejecutó SQL ni modificó datos.
