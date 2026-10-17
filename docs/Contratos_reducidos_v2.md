# SuperMax — Contratos reducidos v2

Fecha: 16/10/2026. Contrato acordado para implementar tres de los cinco servicios de SuperMax_v3.
Este documento es la referencia de mensajes para los handoffs v2. Sustituye sus ejemplos anteriores de reservas.
Describe el comportamiento objetivo: las reservas todavía requieren adaptar el DAO, completar servicios e integrar TCP. No acredita pruebas ni aplicación del SQL.

## Transporte y reglas comunes

- TCP, puerto 5001 por defecto; el servidor admite otro puerto como argumento.
- UTF-8; un objeto JSON completo por línea, terminado en salto de línea.
- Una solicitud y una respuesta por conexión. Los ejemplos multilínea se muestran así para facilitar su lectura; enviarlos compactados en una sola línea.
- `tipo` selecciona el servicio. Usar los nombres en mayúsculas de este documento.
- `requestId` es una cadena no vacía, hasta 100 caracteres, que se copia a la respuesta. No se guarda en la BD ni evita solicitudes duplicadas.
- `ok` es booleano. Los errores llevan `codigo` y `mensaje`; el cliente debe decidir por `codigo`, no por el texto del mensaje.
- Si no se pudo leer requestId como cadena, la respuesta de error lleva `requestId: null`.
- Los SKU distinguen mayúsculas y minúsculas. Los números se envían como números JSON, sin comillas.
- Los SKU, precios e IDs de los ejemplos son ilustrativos; usar productos existentes y el ID efectivamente devuelto.

## 1. GET_CATALOGO

Consulta un SKU o todo el catálogo si `sku` es exactamente la cadena vacía. `sku` debe estar presente y ser una cadena; espacios no equivalen a catálogo completo.

Entrada filtrada:
```json
{"tipo":"GET_CATALOGO","requestId":"cat-01","sku":"ARROZ-1K"}
```
Entrada completa:
```json
{"tipo":"GET_CATALOGO","requestId":"cat-02","sku":""}
```
Respuesta filtrada, suponiendo 10 unidades disponibles:
```json
{
  "ok": true,
  "requestId": "cat-01",
  "productos": [
    {"sku":"ARROZ-1K","nombre":"Arroz 1 kg","precio":8500,"stock":10}
  ],
  "timestamp": "2026-10-16T12:00:00-03:00"
}
```
Un SKU sin coincidencias produce éxito con lista vacía:
```json
{"ok":true,"requestId":"cat-03","productos":[],"timestamp":"2026-10-16T12:00:00-03:00"}
```
Se conserva el formato actual, incluido timestamp. Un producto con disponibilidad cero puede aparecer.
El cambio pendiente es calcular `stock` como disponibilidad: stock físico menos cantidades de reservas RESERVADA. `precio` continúa como entero.

## 2. RESERVAR_STOCK — handoff 1

Aparta unidades sin descontar stock físico. Cada solicitud válida puede crear una nueva reserva.

Entrada:
```json
{
  "tipo": "RESERVAR_STOCK",
  "requestId": "res-01",
  "pedidoExternoId": "PED-101",
  "items": [
    {"sku":"ARROZ-1K","cantidad":3},
    {"sku":"LECHE-1L","cantidad":2}
  ]
}
```
Éxito:
```json
{"ok":true,"requestId":"res-01","reservaId":1,"estado":"RESERVADA"}
```
Validaciones:
- `pedidoExternoId`: cadena no vacía de hasta 50 caracteres. Identifica el pedido externo; no es una FK ni un campo único.
- `items`: array no vacío de objetos con SKU no vacío de hasta 50 caracteres y cantidad entera entre 1 y 2147483647.
- Rechazar cantidades decimales, cadenas numéricas, SKU duplicados y productos inexistentes.
- Todos los productos deben tener disponibilidad suficiente. Guardar cabecera e ítems en una sola transacción; nunca una reserva parcial.

Ejemplo de stock insuficiente:
```json
{"ok":false,"requestId":"res-01","codigo":"SIN_STOCK","mensaje":"Stock insuficiente para ARROZ-1K"}
```
`reservaId` es el entero generado por PostgreSQL. El cliente debe conservarlo para confirmar o liberar.
No se devuelven pedidoExternoId, timestamp ni expiraEnSegundos. No hay vencimiento automático.
Reenviar incluso el mismo requestId puede crear otra reserva si hay disponibilidad; no se promete deduplicación.

## 3. CONFIRMAR_RESERVA — handoff 2

Gestiona una reserva con una de dos acciones: CONFIRMAR o LIBERAR. Ambas pertenecen al mismo servicio.
`reservaId` debe ser un entero positivo hasta 2147483647; `accion` debe coincidir exactamente con una de esas cadenas.

Confirmación:
```json
{"tipo":"CONFIRMAR_RESERVA","requestId":"conf-01","reservaId":1,"accion":"CONFIRMAR"}
```
Éxito:
```json
{"ok":true,"requestId":"conf-01","reservaId":1,"estado":"CONFIRMADA"}
```
Liberación (otra reserva, todavía RESERVADA):
```json
{"tipo":"CONFIRMAR_RESERVA","requestId":"lib-01","reservaId":2,"accion":"LIBERAR"}
```
Éxito:
```json
{"ok":true,"requestId":"lib-01","reservaId":2,"estado":"LIBERADA"}
```

| Estado actual | CONFIRMAR | LIBERAR |
| --- | --- | --- |
| RESERVADA | Descontar físico y cambiar a CONFIRMADA. | Cambiar a LIBERADA sin modificar físico. |
| CONFIRMADA | Éxito, sin volver a descontar. | ESTADO_INVALIDO. |
| LIBERADA | ESTADO_INVALIDO. | Éxito, sin cambios. |

Ejemplo al intentar liberar la reserva 1 ya confirmada:
```json
{"ok":false,"requestId":"lib-02","codigo":"ESTADO_INVALIDO","mensaje":"No se puede liberar una reserva confirmada"}
```
El estado evita el doble descuento; no depende de repetir requestId. Descuentos y cambio de estado se guardan juntos.
No hay EXPIRADA ni reapertura de reservas. Liberar nunca suma al stock físico.

## Errores acordados

| Código | Cuándo se usa |
| --- | --- |
| CAMPO_REQUERIDO | Falta un identificador obligatorio o es una cadena vacía/blanca. |
| SOLICITUD_INVALIDA | Tipos o límites incorrectos; items vacío, cantidades inválidas, SKU duplicados o acción no admitida. |
| PRODUCTO_NO_EXISTE | Uno de los SKU de la reserva no existe. |
| SIN_STOCK | Un producto no tiene disponibilidad suficiente. |
| RESERVA_NO_EXISTE | No existe el reservaId solicitado. |
| ESTADO_INVALIDO | La acción contradice el estado final de la reserva. |
| ERROR_BD | Falló una operación SQL o se detectó inconsistencia de persistencia. |
| JSON_INVALIDO | No pudo interpretarse el texto como JSON. |
| TIPO_NO_SOPORTADO | El tipo solicitado no corresponde a uno de los tres servicios habilitados. |

Ejemplo de reserva inexistente:
```json
{"ok":false,"requestId":"conf-02","codigo":"RESERVA_NO_EXISTE","mensaje":"No existe la reserva indicada"}
```
Ejemplo de cantidad inválida, como 0 o 2.5:
```json
{"ok":false,"requestId":"res-02","codigo":"SOLICITUD_INVALIDA","mensaje":"La cantidad debe ser un entero positivo"}
```
Ejemplo de error de infraestructura:
```json
{"ok":false,"requestId":"res-03","codigo":"ERROR_BD","mensaje":"No se pudo completar la operacion"}
```
No enviar detalles SQL internos. El dispatcher actual todavía clasifica algunos tipos incorrectos como CAMPO_REQUERIDO; ajustar esa distinción al integrar. Esta tabla es el contrato objetivo.

## Ejemplo completo de stock

| Paso | Físico de arroz | Disponible | Estado |
| --- | --- | --- | --- |
| Inicial, sin reservas | 10 | 10 | — |
| Reservar 3 | 10 | 7 | RESERVADA |
| Confirmar esa reserva | 7 | 7 | CONFIRMADA |
| Repetir confirmación | 7 | 7 | CONFIRMADA |

En un escenario independiente con físico 10: reservar 3 deja disponible 7; liberar devuelve disponible 10 y mantiene físico 10.
Una reserva abandonada continúa reteniendo unidades hasta liberarse. Dos creaciones concurrentes de 7 sobre físico 10 no deben reservar 14: transacción y bloqueos coordinan las operaciones.

## Modelo y documentos de implementación

Datos para una demostración: [seed de productos v2](../db/003_seed_productos_v2.sql). Requiere la tabla producto y se ejecuta una vez que se selecciona la base deseada. No sobrescribe SKU existentes ni reinicia su stock al repetirse; no crea reservas.
En una base vacía, ejecutar primero 001_producto.sql y 002_reservas_v2.sql. Si las tablas ya existen, no repetir los scripts de creación.
El seed incluye DEMO-ARROZ-1K con físico inicial 10, DEMO-LECHE-1L con 20, DEMO-AZUCAR-1K con 2 y DEMO-ACEITE-900 con 0. Para usar los ejemplos anteriores, sustituir sus SKU por estos SKU de demostración.

Desde PowerShell, ubicado en SuperMax:
```powershell
& 'C:\Program Files\PostgreSQL\18\bin\psql.exe' -h localhost -U postgres -d supermax -v ON_ERROR_STOP=1 -f .\db\003_seed_productos_v2.sql
```
El SELECT final muestra stock físico; consultar GET_CATALOGO para ver disponibilidad. Los valores disponibles pueden ser menores si ya se reservaron unidades. Para pruebas que reinicien datos, usar una base exclusiva de pruebas.

- [SQL v2](../db/002_reservas_v2.sql): reserva con ID SERIAL, pedido externo y estado; detalles con ID INTEGER, SKU y cantidad.
- [Handoff 1 v2](handoffs/01_reservar_stock_v2.md): creación, catálogo e integración.
- [Handoff 2 v2](handoffs/02_confirmar_liberar_reserva_v2.md): confirmación y liberación.
- [Diseño original v3](SuperMax_v3.md): conserva los cinco servicios originales como antecedente.

GET_PREPARACION y ALERTA_STOCK quedan fuera de esta entrega. El cliente debe adaptarse al reservaId numérico y a la ausencia de vencimiento. El SQL v2 inicializa tablas nuevas; no migra el esquema anterior y no fue ejecutado por esta actualización documental.
