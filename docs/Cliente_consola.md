# Cliente de consola

Ejecutar el main `fpuna.supermax.cliente.ClienteConsola` desde el IDE, o compilar
con `mvn package` en SuperMax y ejecutar:

```powershell
java -cp target/supermax-1.0-SNAPSHOT.jar fpuna.supermax.cliente.ClienteConsola
```

Los clientes conectan siempre a `127.0.0.1:5001`, definido en `ClienteTCP`. No utilizan argumentos de linea de comandos.

El servidor SuperMax y PostgreSQL deben estar disponibles. El main del JAR
sigue siendo el servidor; el cliente se ejecuta en otra consola.

## Menu

1. Mostrar catalogo: consulta todos los productos y su disponibilidad.
2. Buscar producto por SKU: consulta por coincidencia exacta.
3. Crear reserva: muestra el catalogo, pide pedido externo, SKU y cantidad.
   Enter en el SKU termina la carga. Muestra el detalle y pide s/n antes de enviar.
4. Mostrar reservas de esta sesion: historial en memoria y ultimo estado conocido.
5. Confirmar reserva: pide un ID y confirmacion s/n.
6. Liberar reserva: pide un ID y confirmacion s/n.
0. Salir.

El cliente genera requestId y arma los JSON automaticamente. No admite SKU
duplicados ni cantidades no positivas. Los errores del servidor se muestran
sin modificar el historial. No reenvia automaticamente operaciones fallidas.

El historial se pierde al cerrar y no refleja cambios de otros clientes.
Se pueden confirmar/liberar IDs de otras sesiones: tras una respuesta exitosa
aparecen en el historial con pedido desconocido. No existe consulta de reservas
al servidor en este menu. La disponibilidad se vuelve a comprobar en el servidor
al reservar y puede cambiar desde la consulta del catalogo.

`ClienteJson` sigue disponible para pruebas con JSON manual. Ambos clientes
comparten el transporte de `ClienteTCP`.
