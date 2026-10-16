# Handoff 1 — RESERVAR_STOCK e integración del MVP

Fecha: 16/10/2026. Destinatario: desarrollador del servicio 2 (vos). Este documento es un encargo listo para implementar, no una declaración de servicio terminado.

## Objetivo y contexto

Implementar el servicio TCP `RESERVAR_STOCK` de SuperMax. Hoy solo funciona `GET_CATALOGO`; las tablas reserva e items están propuestas, no creadas. EntregaYa consume el catálogo, pero no tiene cliente para reservas. No leer `entrega-01`.

Leer primero [estado y acuerdos compartidos](../Estado_actual.md), [modelo de BD](../Base_de_datos.md), [SQL de reservas](../../db/002_reservas_propuesta.sql) y [contratos v3](../SuperMax_v3.md). Los acuerdos compartidos fijan tipos, errores, estados, reintentos y bloqueos para ambas implementaciones.

## Alcance y archivos propios

- Crear `src/main/java/fpuna/supermax/servicio/ReservaStockServicio.java` y `bd/ReservaStockDAO.java` bajo el mismo paquete base.
- Exponer `public JSONObject ejecutar(JSONObject solicitud) throws SQLException` en el servicio. Devolver respuestas completas con `ok` y `requestId`; propagar fallos SQL después del rollback.
- Integrar el case `RESERVAR_STOCK` en `TCPServerHilo`, delegando al servicio. El handler continúa encargándose de leer/escribir JSON por línea.
- Ser responsable de la integración final de `CONFIRMAR_RESERVA` cuando esté disponible `GestionReservaServicio` del compañero. No implementarlo en su lugar.
- Ajustar las consultas del catálogo para disponibilidad y coordinar la aplicación del SQL en la base de desarrollo. En pruebas usar una base separada.
- Preservar el cambio local previo de `ProductoDAO.java`: ya propaga SQLException. No volver a convertir errores SQL en listas vacías.

## Entrada y salida

```json
{
  "tipo": "RESERVAR_STOCK",
  "requestId": "req-reserva-01",
  "pedidoExternoId": "PED-101",
  "items": [{"sku": "ARROZ-1K", "cantidad": 3}]
}
```

```json
{
  "ok": true,
  "requestId": "req-reserva-01",
  "reservaId": "RES-uuid-generado-en-java",
  "pedidoExternoId": "PED-101",
  "estado": "RESERVADA",
  "expiraEnSegundos": 300,
  "timestamp": "2026-10-16T12:00:00-03:00"
}
```

Los valores de ejemplo no son IDs ni existencias garantizados en la base. Generar el ID con `RES-` + UUID; cabe en VARCHAR(50). Para nuevas reservas, el plazo inicial es 300 segundos; para reintentos, devolver el plazo restante no negativo y estado actual. Calcular el plazo con el vencimiento almacenado y la hora de BD; puede haber transcurrido parte del plazo antes de responder.

## Flujo de implementación

1. Validar antes de escribir: requestId no vacío (máximo 100), pedidoExternoId no vacío (máximo 50), items array no vacío, SKU cadena no vacía (máximo 50), cantidades enteras positivas hasta Integer.MAX_VALUE. Con json-simple no truncar un Double a entero ni convertir un Long fuera de rango. Rechazar SKU duplicado; la comparación de contenido para reintentos ignora el orden del array.
2. Consultar por request_id. Si existe y coincide pedido/items, devolver la reserva original, sin renovar ni insertar. Si difiere, `REQUEST_ID_CONFLICTO`. Si está RESERVADA pero vencida, devolverla con estado efectivo EXPIRADA y plazo 0, sin reactivarla; no hace falta actualizar el estado almacenado en este camino.
3. Para una creación nueva, abrir una conexión, aislamiento READ_COMMITTED, desactivar autoCommit. Los DAO reciben esa Connection.
4. Bloquear productos con FOR UPDATE en orden ascendente de SKU. Si alguno no existe, rollback y `PRODUCTO_NO_EXISTE`.
5. Después de adquirir bloqueos, consultar disponibilidad en una nueva sentencia: stock físico menos sumas de reservas activas y no vencidas. Evaluar la hora después de la espera. Si un item no alcanza, rollback y `SIN_STOCK`, sin cabecera ni items parciales.
6. Obtener hora actual de PostgreSQL, insertar cabecera RESERVADA y vencimiento a 300 segundos, insertar todos los items. No modificar producto.stock. Commit y respuesta.
7. Si otro hilo creó el mismo requestId, la restricción UNIQUE arbitra. Tras el rollback por esa restricción específica, volver a consultar y comparar pedido/items; devolver la reserva original o conflicto. No tratar cualquier SQLException como reintento ni continuar usando una transacción abortada.
8. Ante cualquier otro fallo SQL, rollback y dejar que el dispatcher devuelva ERROR_BD. Cerrar Connection, Statements y ResultSets con try-with-resources.

## Catálogo e integración compartida

- Modificar `ProductoDAO.seleccionarCatalogo()` y `seleccionarPorSku()` para devolver disponibilidad con una sola consulta agregada por llamada, sin N+1 consultas. Usar SUM/COALESCE y excluir vencidas por fecha, no solo por estado.
- Mantener la forma JSON, `ok: true` con lista vacía y la propagación de SQLException. No filtrar automáticamente productos con stock disponible 0.
- No reutilizar el método de catálogo como fuente de stock físico para confirmar: el compañero necesita leer la columna física directamente.
- Al integrar ambos cases, generalizar el mensaje/log del catch SQL actual, que menciona solo catálogo. Mantener el campo requestId para todos los servicios.
- Incorporar el segundo case únicamente después de integrar la clase del compañero: `return new GestionReservaServicio().ejecutar(solicitud).toJSONString();`. El propio case sigue el mismo patrón con ReservaStockServicio.
- Las pruebas TCP pueden usar un cliente de prueba separado; no convertir ahora el Main interactivo de EntregaYa en un menú de todos los servicios.

## Pruebas de aceptación

Usar existencias controladas en una base exclusiva para pruebas. Estas son pruebas requeridas para la implementación futura; no se ejecutaron al redactar el handoff.

| Caso | Resultado esperado |
| --- | --- |
| Stock físico 10, reservar 3 | Una reserva, un item de 3, físico 10, catálogo 7. |
| Varios SKUs, todos disponibles | Una cabecera y todos los items, commit completo. |
| Un SKU sin unidades suficientes | SIN_STOCK y ninguna inserción parcial. |
| SKU inexistente / items vacío / repetidos / cantidad decimal, negativa o fuera de rango | Error controlado sin escrituras. |
| Dos solicitudes simultáneas por 7 sobre stock 10 | Solo una tiene éxito; la otra recibe SIN_STOCK; no reservar 14. |
| Misma solicitud repetida, también concurrentemente | Mismo reservaId y una sola reserva, mismo vencimiento. |
| Mismo requestId con otro pedido o items | REQUEST_ID_CONFLICTO. |
| Reintentar después de vencer o confirmar | No crear otra reserva ni renovar el plazo. |
| Fallo durante inserción de items | Rollback de cabecera e items. |
| GET_CATALOGO completo, filtrado y sin coincidencias | Formato conservado; disponibilidad correcta; lista vacía exitosa. |

El compañero puede proporcionar fixtures de reservas confirmadas/liberadas; hasta entonces podés preparar esos estados en la base de pruebas. Verificar también el catálogo mientras se confirma una reserva concurrentemente, sin resultados que descuenten dos veces la misma cantidad.

## Entrega esperada

Código y pruebas propios, integración TCP, catálogo adaptado, comandos reproducibles de preparación de la base de pruebas y resultados. Ejecutar `mvn package` (o `mvn clean package` si se movieron packages), y pruebas de BD/concurrencia; un build solo no prueba la atomicidad. Coordinar dependencias de pruebas y cambios del POM como integrador.

Actualizar estado/documentación cuando el código y el SQL estén efectivamente implementados/aplicados. Entregar al compañero los archivos SQL definitivos y un ejemplo de solicitud/respuesta. No marcar completo mientras falte integrar y probar el servicio 3 con su implementación.
