# SuperMax

Subsistema de gestión de supermercado del sistema distribuido del laboratorio de Sistemas Distribuidos (FPUNA). Maneja catálogo de productos, stock y precios.

Forma parte de un sistema compuesto por dos organizaciones autónomas que se comunican por paso de mensajes sobre sockets TCP/UDP. La contraparte de este subsistema es **[EntregaYa](https://github.com/mbarakajap/EntregaYa)**.

> Estado al 17/10/2026: implementados e integrados por TCP `GET_CATALOGO`, `RESERVAR_STOCK` y `CONFIRMAR_RESERVA`, con persistencia JDBC en PostgreSQL. Compilación y validaciones de gestión verificadas; se realizaron pruebas manuales con JSON y queda completar y registrar las pruebas conjuntas de BD, rollback y concurrencia. Referencia vigente: [contratos reducidos v2](docs/Contratos_reducidos_v2.md).

El contrato reducido usa reservaId numérico y estados RESERVADA, CONFIRMADA y LIBERADA. No hay vencimiento ni deduplicación de creaciones por requestId: una reserva permanece hasta confirmarse o liberarse, y repetir una creación puede generar otra reserva.

## Trabajo dividido

- [Handoff 1 v2 — Reservar stock e integración](docs/handoffs/01_reservar_stock_v2.md).
- [Handoff 2 v2 — Confirmar o liberar reserva](docs/handoffs/02_confirmar_liberar_reserva_v2.md).
- [Contratos y ejemplos v2](docs/Contratos_reducidos_v2.md), [SQL de reservas v2](db/002_reservas_v2.sql) y [cliente de pruebas](docs/Cliente_json.md).

Los handoffs describen encargos de implementación; no sustituyen los resultados de pruebas. Ver [estado actual](docs/Estado_actual.md), [modelo de BD vigente](docs/Base_de_datos.md) y [evidencia del handoff 2](docs/entrega_handoff_02_v2.md). Los handoffs originales y el diseño v3 están marcados como históricos.

## Stack

- Java 1.8
- Apache Maven (fat JAR con `maven-shade-plugin`)
- PostgreSQL 12+
- Servidor de sockets TCP, consumido por EntregaYa; UDP pendiente
- JSON via `json-simple 1.1.1`
- Driver JDBC `postgresql 42.7.4`

## Prerrequisitos

- JDK 1.8 instalado y disponible en `PATH`
- Maven 3.6 o superior
- PostgreSQL accesible en `localhost:5432`, base `supermax`, con las tablas `producto`, `reserva` y `reserva_item` del modelo v2. La conexión JDBC está configurada en [Bd.java](src/main/java/fpuna/supermax/bd/Bd.java).

## Setup

### 1. Clonar el repositorio

```bash
git clone https://github.com/riverondev/SuperMax.git
cd SuperMax
```

### 2. Preparar la base de datos

Crear la base `supermax` y revisar la configuración de conexión. En una base vacía, ejecutar en este orden desde pgAdmin o psql:

1. [001_producto.sql](db/001_producto.sql): tabla de productos.
2. [002_reservas_v2.sql](db/002_reservas_v2.sql): cabecera y detalles de reservas.
3. [003_seed_productos_v2.sql](db/003_seed_productos_v2.sql): productos de demostración, opcional.

Si las tablas ya existen, no repetir los scripts de creación. El SQL v2 no migra el esquema anterior; `002_reservas_propuesta.sql` es histórico y no debe ejecutarse junto con v2.

El seed agrega 20 productos de almacen, lacteos, carnes, frutas, verduras, bebidas, limpieza e higiene. Conserva los existentes y no reinicia su stock al repetirse. Incluye arroz con stock inicial 40, leche con 30, azucar con 2, jugo con 1 y aceite con 0. Los ensayos que reinicien datos deben usar una base exclusiva de pruebas.

`producto.stock` es stock físico. El campo `stock` del catálogo es disponibilidad: físico menos cantidades de reservas RESERVADA. Reservar no cambia el físico; confirmar descuenta una sola vez; liberar no lo incrementa. Los DAO controlan las transacciones y bloquean productos en orden de SKU para coordinar operaciones concurrentes.

### 3. Compilar y empaquetar

```bash
mvn clean package
```

Esto genera un fat JAR ejecutable en `target/supermax-1.0-SNAPSHOT.jar` con todas las dependencias incluidas.

### 4. Ejecutar el servidor

```bash
java -jar target/supermax-1.0-SNAPSHOT.jar
```

El servidor escucha en el puerto `5001` por defecto. Se puede cambiar pasando el puerto como primer argumento. Atiende una solicitud y una respuesta JSON por conexión, ambas terminadas en salto de línea y codificadas en UTF-8.

Usar el JAR empaquetado con dependencias. `original-supermax-1.0-SNAPSHOT.jar` y `target/classes` por sí solos no incluyen json-simple ni el driver JDBC.

### 5. Ejecutar el cliente de pruebas

Con el servidor iniciado, abrir otra terminal en SuperMax:

```powershell
java -cp target/supermax-1.0-SNAPSHOT.jar fpuna.supermax.cliente.ClienteJson
```

Los clientes conectan siempre a `127.0.0.1:5001`, definido en `ClienteTCP`. No utilizan argumentos de linea de comandos.

Pegar cada JSON en una sola línea después de `JSON>`. El cliente muestra la respuesta y permite enviar otra solicitud; escribir `salir` para terminar. No reenvía automáticamente solicitudes fallidas.

Ejemplo con el seed:

```json
{"tipo":"GET_CATALOGO","requestId":"cat-01","sku":"ARROZ-1K"}
```

Ver [uso del cliente](docs/Cliente_json.md) y [contratos v2](docs/Contratos_reducidos_v2.md) para reservar, confirmar, liberar y probar errores.

## Estructura del proyecto

```
SuperMax/
├── pom.xml                              Configuración de Maven y dependencias
├── db/                                  Esquema y seed
├── docs/                                Contratos, ejemplos y handoffs
├── src/
│   ├── main/
│   │   ├── java/fpuna/supermax/         Código fuente (paquete base)
│   │   └── resources/                   Recursos (configs, etc.)
│   └── test/
│       └── java/                        Validación e integración de gestión
└── target/                              Generado por Maven (gitignored)
```

Base package: `fpuna.supermax`. Toda clase Java debe estar bajo `src/main/java/fpuna/supermax/` y declarar `package fpuna.supermax[.subpaquete];`.

## Comunicación distribuida

- **GET_CATALOGO**: consulta completa con SKU vacío o filtrada por coincidencia exacta, devolviendo disponibilidad.
- **RESERVAR_STOCK**: aparta uno o varios productos y devuelve un ID numérico de reserva.
- **CONFIRMAR_RESERVA**: confirma o libera según `accion`; una confirmación repetida no vuelve a descontar.
- **Fuera de esta entrega**: GET_PREPARACION y ALERTA_STOCK por UDP.

Los mensajes vigentes están en [Contratos reducidos v2](docs/Contratos_reducidos_v2.md). [SuperMax_v3.md](docs/SuperMax_v3.md) conserva el diseño original de cinco servicios.

## Verificación

`mvn clean package` compila y empaqueta. Las comprobaciones existentes de gestión son programas con `main`; Maven las compila, pero no las ejecuta automáticamente.

Para ejecutar las validaciones sin acceder a PostgreSQL, desde PowerShell:

```powershell
java -cp "target/test-classes;target/supermax-1.0-SNAPSHOT.jar" fpuna.supermax.bd.GestionReservaValidacion
```

`GestionReservaIntegracion` requiere el esquema v2 en una base exclusiva llamada `supermax_gestion_test`, configurada mediante `GESTION_TEST_URL`, `GESTION_TEST_USER` y `GESTION_TEST_PASSWORD`. Sus fixtures vacían las tablas de esa base; no ejecutarlos sobre desarrollo.

Antes de cerrar la entrega, registrar el recorrido catálogo → reserva → confirmación/liberación, rollback y concurrencia. Caso de referencia: físico 10 → reservar 3: disponible 7, físico 10 → confirmar: disponible 7, físico 7. Repetir confirmación conserva físico 7. En un escenario independiente, liberar devuelve la disponibilidad sin cambiar el físico.

## Licencia

Proyecto académico — uso educativo, Facultad Politécnica UNA.
