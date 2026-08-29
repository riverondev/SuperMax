# SuperMax

Subsistema de gestión de supermercado del sistema distribuido del laboratorio de Sistemas Distribuidos (FPUNA). Maneja catálogo de productos, stock y precios.

Forma parte de un sistema compuesto por dos organizaciones autónomas que se comunican por paso de mensajes sobre sockets TCP/UDP. La contraparte de este subsistema es **[EntregaYa](https://github.com/mbarakajap/EntregaYa)**.

> Estado actual: bootstrap. El proyecto tiene la configuración base de Maven y la estructura de packages. La clase `fpuna.supermax.Main` referenciada en `pom.xml` aún no está implementada (es un placeholder).

## Stack

- Java 1.8
- Apache Maven (fat JAR con `maven-shade-plugin`)
- PostgreSQL 12+
- Sockets TCP (cliente hacia EntregaYa) y UDP
- JSON via `json-simple 1.1.1`
- Driver JDBC `postgresql 42.7.4`

## Prerrequisitos

- JDK 1.8 instalado y disponible en `PATH`
- Maven 3.6 o superior
- PostgreSQL 12 o superior corriendo en `localhost:5432` (será necesario cuando se defina el esquema de la base)

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

> La clase `Main` es un placeholder. Cuando la implementes, asegurate de que su FQN coincida con `<mainClass>` en `pom.xml` (actualmente `fpuna.supermax.Main`).

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

- **TCP**: cliente que consume servicios de EntregaYa (consulta de catálogo, reserva de stock, etc.).
- **UDP**: servicios auxiliares internos.

Los contratos JSON de cada servicio (request/response) están documentados en [`docs/SuperMax_v3.md`](docs/SuperMax_v3.md).

## Licencia

Proyecto académico — uso educativo, Facultad Politécnica UNA.
