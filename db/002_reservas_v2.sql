-- Esquema reducido de reservas: handoffs v2.
-- Requiere public.producto (001_producto.sql solo en una base vacia).
-- Ejecutar en una base que todavia no tenga reserva ni reserva_item.
-- Sustituye a 002_reservas_propuesta.sql; NO ejecutar ambos scripts.
-- No migra ni elimina tablas o datos del esquema anterior.
-- requestId solo viaja en JSON. No hay fechas ni vencimiento.

BEGIN;

CREATE TABLE public.reserva (
    reserva_id SERIAL PRIMARY KEY,
    pedido_externo_id VARCHAR(50) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'RESERVADA',

    CONSTRAINT ck_reserva_estado CHECK (
        estado IN ('RESERVADA', 'CONFIRMADA', 'LIBERADA')
    )
);

CREATE TABLE public.reserva_item (
    reserva_id INTEGER NOT NULL,
    sku VARCHAR(50) NOT NULL,
    cantidad INTEGER NOT NULL,

    CONSTRAINT pk_reserva_item PRIMARY KEY (reserva_id, sku),
    CONSTRAINT fk_reserva_item_reserva FOREIGN KEY (reserva_id)
        REFERENCES public.reserva (reserva_id),
    CONSTRAINT fk_reserva_item_producto FOREIGN KEY (sku)
        REFERENCES public.producto (sku),
    CONSTRAINT ck_reserva_item_cantidad CHECK (cantidad > 0)
);

CREATE INDEX ix_reserva_item_sku ON public.reserva_item (sku);

COMMIT;
