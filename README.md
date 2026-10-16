# SuperMax

Subsistema de gestión de supermercado del sistema distribuido del laboratorio de Sistemas Distribuidos (FPUNA). Maneja catálogo de productos, stock y precios.

Forma parte de un sistema compuesto por dos organizaciones autónomas que se comunican por paso de mensajes sobre sockets TCP/UDP. La contraparte de este subsistema es **[EntregaYa](https://github.com/mbarakajap/EntregaYa)**.

> Estado revisado al 16/10/2026: servidor TCP con `GET_CATALOGO` implementado y persistencia JDBC en PostgreSQL. `RESERVAR_STOCK` y `CONFIRMAR_RESERVA` están diseñados, pero pendientes de implementar. Ver [estado y acuerdos del MVP](docs/Estado_actual.md).

## Trabajo dividido

- [Handoff 1 — Reservar stock e integración](docs/handoffs/01_reservar_stock.md).
- [Handoff 2 — Confirmar o liberar reserva](docs/handoffs/02_confirmar_liberar_reserva.md).
- [Modelo de base de datos](docs/Base_de_datos.md) y [SQL propuesto](db/002_reservas_propuesta.sql).

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
- PostgreSQL accesible en `localhost:5432`, base `supermax`, con la tabla `public.producto`. La conexión JDBC está configurada en `src/main/java/fpuna/supermax/bd/Bd.java`.

## Setup

### 1. Clonar el repositorio

```bash
git clone https://github.com/riverondev/SuperMax.git
cd SuperMax
```

### 2. Compilar y empaquetar

```bash
mvn clean package
```

Esto genera un fat JAR ejecutable en `target/supermax-1.0-SNAPSHOT.jar` con todas las dependencias incluidas.

### 3. Ejecutar

```bash
java -jar target/supermax-1.0-SNAPSHOT.jar
```

El servidor escucha en el puerto `5001` por defecto. Se puede cambiar pasando el puerto como primer argumento. Atiende una solicitud y una respuesta JSON por conexión, ambas terminadas en salto de línea y codificadas en UTF-8.

La tabla `producto` ya existe en la base local. `db/001_producto.sql` documenta su definición para bases vacías; `db/002_reservas_propuesta.sql` todavía no se aplicó. Consultar [los acuerdos de implementación](docs/Estado_actual.md) antes de incorporar reservas.

## Estructura del proyecto

```
SuperMax/
├── pom.xml                              Configuración de Maven y dependencias
├── src/
│   ├── main/
│   │   ├── java/fpuna/supermax/         Código fuente (paquete base)
│   │   └── resources/                   Recursos (configs, etc.)
│   └── test/
│       └── java/                        Tests unitarios
└── target/                              Generado por Maven (gitignored)
```

Base package: `fpuna.supermax`. Toda clase Java debe estar bajo `src/main/java/fpuna/supermax/` y declarar `package fpuna.supermax[.subpaquete];`.

## Comunicación distribuida

- **TCP implementado**: SuperMax provee `GET_CATALOGO`; EntregaYa lo consume. SKU vacío consulta todo el catálogo; SKU informado filtra por coincidencia exacta.
- **TCP pendiente**: reserva de stock, confirmación/liberación y consulta de preparación.
- **UDP pendiente**: alerta de stock agotado.

Los contratos JSON de cada servicio (request/response) están documentados en [`docs/SuperMax_v3.md`](docs/SuperMax_v3.md).

## Licencia

Proyecto académico — uso educativo, Facultad Politécnica UNA.
