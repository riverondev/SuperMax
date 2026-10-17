# Handoff 1 v2 — Reservar stock e integrar el catálogo

Fecha: 16/10/2026. Alcance académico: GET_CATALOGO, RESERVAR_STOCK y CONFIRMAR_RESERVA.
Este documento define trabajo pendiente; no acredita código implementado ni SQL aplicado.
No leer `entrega-01`. Ver también [handoff 2 v2](02_confirmar_liberar_reserva_v2.md).

## Contrato reducido compartido

Esta v2 reemplaza los requisitos del handoff 1 original para este incremento.
En caso de diferencias con Estado_actual.md, Base_de_datos.md y SuperMax_v3.md, aplicar aquí el alcance reducido; actualizar esos documentos al integrar.
Conservar Java 8, JDBC, json-simple y mensajes JSON por línea sobre TCP.
requestId identifica la solicitud en la respuesta; no se persiste ni evita duplicados.
No implementar vencimiento, EXPIRADA, timestamps nuevos, scheduler ni comparación de reintentos.
Una reserva permanece activa hasta confirmarse o liberarse. Reenviar una creación puede crear otra reserva.

Entrada:
```json
{"tipo":"RESERVAR_STOCK","requestId":"req-01","pedidoExternoId":"PED-101","items":[{"sku":"ARROZ-1K","cantidad":3}]}
```
Éxito:
```json
{"ok":true,"requestId":"req-01","reservaId":1,"estado":"RESERVADA"}
```
Error:
```json
{"ok":false,"requestId":"req-01","codigo":"SIN_STOCK","mensaje":"Stock insuficiente para ARROZ-1K"}
```
reservaId es ahora un entero positivo generado por PostgreSQL. pedidoExternoId no se repite en la respuesta.
Errores: CAMPO_REQUERIDO, SOLICITUD_INVALIDA, PRODUCTO_NO_EXISTE, SIN_STOCK y ERROR_BD.
Errores incluyen requestId cuando pudo leerse como cadena; en otro caso null.

## Modelo común para ambos desarrolladores

- producto se conserva: sku VARCHAR(50) PK, nombre, precio y stock físico.
- reserva: reserva_id SERIAL PRIMARY KEY, pedido_externo_id VARCHAR(50) NOT NULL, estado VARCHAR(20) NOT NULL DEFAULT 'RESERVADA'.
- CHECK de estado: RESERVADA, CONFIRMADA o LIBERADA. pedido_externo_id no es FK ni UNIQUE.
- reserva_item: reserva_id INTEGER NOT NULL FK a reserva, sku VARCHAR(50) NOT NULL FK a producto, cantidad INTEGER NOT NULL CHECK (cantidad > 0).
- PK de reserva_item: (reserva_id, sku). Índice por sku. Sin borrado en cascada.
- No guardar request_id, creada_en ni expira_en. No agregar tablas de pedidos, inventario o auditoría.
- Reserva Java: Integer reservaId, String pedidoExternoId, String estado, List<ReservaItem> items.
- ReservaItem Java: Integer reservaId, String sku, Integer cantidad. Sin fechas ni lógica SQL.

El integrador prepara el SQL definitivo y lo comparte. Primero inspeccionar si ya se aplicó el esquema anterior: no ejecutar DROP ni convertir IDs existentes a ciegas. Crear una base de pruebas con el nuevo esquema; si hay datos previos, acordar una migración que los preserve antes de cambiar desarrollo. El antiguo 002_reservas_propuesta.sql no corresponde a v2.

## Organización y tamaño

Usar Producto como referencia: entidades simples, consultas en DAO y validación/respuestas en servicio.
Objetivo y límite de revisión para archivos Java nuevos: hasta 100 líneas físicas legibles por archivo.
No juntar instrucciones, eliminar espacios o esconder lógica en SQL enorme para cumplir el límite.
Si una clase no cabe, revisar el diseño antes de seguir; separar una responsabilidad real, sin crear frameworks ni clases por cada método.
Para que ambos trabajen sin editar un DAO grande compartido, usar ReservaStockDAO y GestionReservaDAO. Esta separación sustituye la idea preliminar de un único ReservaDAO.
Cada DAO abre y cierra su conexión dentro de su operación pública; sus auxiliares reciben esa misma conexión.
No abrir conexiones desde los servicios ni una conexión por consulta dentro de una reserva.

Archivos propios: servicio/ReservaStockServicio.java y bd/ReservaStockDAO.java.
El integrador mantiene entidades, SQL, ProductoDAO y TCPServerHilo; el compañero recibe sus firmas y no las modifica simultáneamente.
Servicio: public JSONObject ejecutar(JSONObject solicitud) throws SQLException.
DAO: public Reserva crearReserva(String pedidoExternoId, List<ReservaItem> items) throws SQLException.
Para fallos de negocio, acordar una pequeña ReservaException con codigo y mensaje, capturada por el servicio; SQLException se propaga al dispatcher. No devolver false indistintamente para falta de stock y fallo SQL.
Compartir esa excepción antes del trabajo paralelo. Extraer validación o construcción de respuestas solo si evita repetición y ayuda al límite de tamaño.

## Implementación paso a paso

1. Validar requestId y pedidoExternoId como cadenas no vacías, límites 100 y 50. Validar items como array no vacío, objetos con SKU no vacío de hasta 50 caracteres y cantidad entera entre 1 e Integer.MAX_VALUE. Rechazar SKU repetidos; distinguir mayúsculas. Validar antes de escribir.
2. Convertir items a entidades y ordenarlos por SKU. El método público del DAO abre Connection, configura READ_COMMITTED y autoCommit=false.
3. Bloquear todos los productos antes de comprobar disponibilidad: SELECT ... FOR UPDATE, uno por SKU en el orden acordado. Si falta alguno, abortar con PRODUCTO_NO_EXISTE.
4. En sentencias posteriores a los bloqueos, calcular disponible = producto.stock - SUM de cantidades con reserva.estado='RESERVADA'. COALESCE permite cero reservas. Se admiten consultas por producto para mantener esta primera implementación clara.
5. Si falta disponibilidad, rollback y SIN_STOCK. No dejar cabecera ni ítems parciales.
6. Insertar cabecera RESERVADA y recuperar reserva_id con INSERT ... RETURNING reserva_id. Insertar ítems con un PreparedStatement reutilizado y un bucle sencillo; no hace falta batch.
7. Commit y devolver Reserva. No modificar producto.stock. Ante error SQL o de negocio, rollback; cerrar recursos con try-with-resources. Si rollback falla, conservar también el error original.
8. El servicio construye la respuesta JSON. Incorporar el case TCP solo cuando el servicio compile; generalizar mensajes ERROR_BD y tipos soportados del dispatcher.

Mantener todos los bloqueos hasta commit/rollback. El orden común es productos ordenados por SKU y después reserva existente. Para una creación no se bloquean cabeceras ajenas.

## Catálogo e integración

Producto conserva sus campos. Cambiar sus consultas para que el stock enviado sea disponibilidad; conservar el JSON actual, incluso su timestamp, lista vacía exitosa y SQLException.
Usar una consulta agregada por llamada de catálogo, con LEFT JOIN y COALESCE; esto evita lecturas mezcladas al confirmar concurrentemente. No ocultar productos con disponibilidad cero.
El stock físico para confirmar se lee directamente de producto, nunca del resultado del catálogo.
Si TCPServerHilo crece demasiado, extraer el armado de respuestas del catálogo a una clase pequeña; no reescribir el transporte por estética.
Integrar GestionReservaServicio del compañero y actualizar contratos v3, estado, modelo y notas SQL para que reflejen esta v2.
El consumidor debe conocer que reservaId cambió de cadena a número y que no hay vencimiento.

## Verificación y entrega

Usar una base exclusiva de pruebas, sin fixtures destructivos en supermax.
- Físico 10, reservar 3: una cabecera, un ítem, físico 10 y catálogo 7.
- Varios productos: se guardan todos; si uno falta o no alcanza, no se guarda ninguno.
- Cantidad inválida o SKU repetido: error controlado sin escrituras.
- Dos conexiones reservando 7 sobre 10: solo una tiene éxito.
- Fallo al insertar un ítem: rollback de toda la reserva.
- Integración con handoff 2: confirmar deja físico/disponible 7; liberar en otro escenario devuelve disponibilidad 10.
Ejecutar mvn package y registrar resultados reales. Un build no sustituye las comprobaciones de BD.
Entregar código legible, SQL reproducible, ejemplos TCP y lista breve de pruebas pasadas/pendientes. Revisar el límite de 100 líneas antes de dar cada archivo nuevo por terminado.
