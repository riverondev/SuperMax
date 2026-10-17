-- Productos de demostracion para los contratos reducidos v2.
-- Requiere public.producto. No crea reservas ni modifica las existentes.
-- Puede repetirse: los SKU ya existentes conservan todos sus valores.
-- Los stocks indicados son iniciales; repetir el seed no los reinicia.

BEGIN;

INSERT INTO public.producto (sku, nombre, precio, stock) VALUES
    ('DEMO-ARROZ-1K', 'Arroz 1 kg', 8500, 10),
    ('DEMO-LECHE-1L', 'Leche 1 litro', 7000, 20),
    ('DEMO-AZUCAR-1K', 'Azucar 1 kg', 6500, 2),
    ('DEMO-ACEITE-900', 'Aceite 900 ml', 12000, 0)
ON CONFLICT (sku) DO NOTHING;

COMMIT;

SELECT sku, nombre, precio, stock AS stock_fisico
FROM public.producto
WHERE sku IN (
    'DEMO-ARROZ-1K', 'DEMO-LECHE-1L',
    'DEMO-AZUCAR-1K', 'DEMO-ACEITE-900'
)
ORDER BY sku;
