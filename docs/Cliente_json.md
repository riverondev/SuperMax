# Cliente TCP para pruebas manuales

Clase: `fpuna.supermax.cliente.ClienteJson`. Compatible con Java 8, sin dependencias adicionales.
Envia literalmente cada linea introducida y muestra la respuesta del servidor. Permite tambien probar JSON mal formado y campos invalidos.
Abre una conexion por solicitud, usa UTF-8 y espera hasta 5 segundos al conectar y 10 al leer. Ante un error vuelve al prompt y no reintenta automaticamente.

Desde la carpeta SuperMax, compilar:
```powershell
mvn package
```
En una terminal, iniciar el servidor:
```powershell
java -jar target/supermax-1.0-SNAPSHOT.jar
```
En otra terminal, iniciar el cliente:
```powershell
java -cp target/supermax-1.0-SNAPSHOT.jar fpuna.supermax.cliente.ClienteJson
```
Destino opcional (por defecto 127.0.0.1:5001):
```powershell
java -cp target/supermax-1.0-SNAPSHOT.jar fpuna.supermax.cliente.ClienteJson 127.0.0.1 5001
```
Pegar un JSON por linea despues del prompt `JSON>`. No escribir el prompt ni las comillas de un bloque Markdown. Escribir `salir` para terminar; un fin de entrada tambien cierra el cliente.

## Ejemplos con el seed v2

Consultar disponibilidad:
```json
{"tipo":"GET_CATALOGO","requestId":"cat-01","sku":"DEMO-ARROZ-1K"}
```
Crear una reserva (se guarda realmente en la base configurada por el servidor):
```json
{"tipo":"RESERVAR_STOCK","requestId":"res-01","pedidoExternoId":"PED-DEMO-01","items":[{"sku":"DEMO-ARROZ-1K","cantidad":3}]}
```
Guardar el reservaId de la respuesta y sustituir el 1 de este ejemplo:
```json
{"tipo":"CONFIRMAR_RESERVA","requestId":"conf-01","reservaId":1,"accion":"CONFIRMAR"}
```
Para liberar una reserva que siga RESERVADA, usar su ID y accion LIBERAR. Una confirmada no se puede liberar.
Las creaciones no se deduplican por requestId: repetir RESERVAR_STOCK puede crear otra reserva. Los stocks iniciales del seed no se reinician al repetirlo.

Prueba del tipo incorrecto de requestId, sin crear reservas:
```json
{"tipo":"GET_CATALOGO","requestId":123,"sku":"DEMO-ARROZ-1K"}
```
Contrato completo: [Contratos reducidos v2](Contratos_reducidos_v2.md).
