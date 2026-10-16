-- PROPUESTA para el MVP: pendiente de implementar y aplicar.
-- Requiere public.producto, que ya existe en la base local supermax.
-- No modifica producto y no crea una tabla de pedidos de EntregaYa.
-- reserva_id y request_id se generan/reciben en el servicio Java.

BEGIN;

CREATE TABLE public.reserva (
    reserva_id VARCHAR(50) NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    pedido_externo_id VARCHAR(50) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'RESERVADA',
    creada_en TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_en TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_reserva PRIMARY KEY (reserva_id),
    CONSTRAINT uq_reserva_request_id UNIQUE (request_id),
    CONSTRAINT ck_reserva_estado CHECK (
        estado IN ('RESERVADA', 'CONFIRMADA', 'LIBERADA', 'EXPIRADA')
    ),
    CONSTRAINT ck_reserva_vencimiento CHECK (expira_en > creada_en)
);

CREATE TABLE public.reserva_item (
    reserva_id VARCHAR(50) NOT NULL,
    sku VARCHAR(50) NOT NULL,
    cantidad INTEGER NOT NULL,

    CONSTRAINT pk_reserva_item PRIMARY KEY (reserva_id, sku),
    CONSTRAINT fk_reserva_item_reserva FOREIGN KEY (reserva_id)
        REFERENCES public.reserva (reserva_id),
    CONSTRAINT fk_reserva_item_producto FOREIGN KEY (sku)
        REFERENCES public.producto (sku),
    CONSTRAINT ck_reserva_item_cantidad CHECK (cantidad > 0)
);

-- Para calcular las unidades reservadas de un SKU sin recorrer todos los items.
-- Las claves primarias ya crean indices para reserva_id y (reserva_id, sku).
CREATE INDEX ix_reserva_item_sku ON public.reserva_item (sku);

COMMIT;
