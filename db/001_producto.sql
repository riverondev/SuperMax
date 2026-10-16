-- Definicion equivalente de public.producto, verificada en PostgreSQL el 03/10/2026.
-- Para inicializar una base vacia; la tabla ya existe en la base local supermax.
-- Este archivo no se ejecuto sobre la base existente.

CREATE TABLE public.producto (
    sku VARCHAR(50) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    precio BIGINT NOT NULL,
    stock INTEGER NOT NULL,

    CONSTRAINT pk_sku PRIMARY KEY (sku),
    CONSTRAINT ck_precio_positivo CHECK (precio >= 0),
    CONSTRAINT ck_stock_no_negativo CHECK (stock >= 0)
);
