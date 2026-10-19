-- Catalogo inicial variado para SuperMax. Precios en guaranies.
-- Requiere public.producto. No crea reservas ni modifica las existentes.
-- Puede repetirse: los SKU ya existentes conservan todos sus valores.
-- Los stocks indicados son iniciales; repetir el seed no los reinicia.

BEGIN;

INSERT INTO public.producto (sku, nombre, precio, stock) VALUES
    ('ARROZ-1K', 'Arroz 1 kg', 8500, 40),
    ('LECHE-1L', 'Leche entera 1 litro', 7000, 30),
    ('AZUCAR-1K', 'Azucar 1 kg', 6500, 2),
    ('ACEITE-900', 'Aceite de girasol 900 ml', 12000, 0),
    ('HARINA-1K', 'Harina de trigo 1 kg', 6000, 25),
    ('FIDEO-500', 'Fideos tallarin 500 g', 5500, 35),
    ('YERBA-500', 'Yerba mate 500 g', 14000, 18),
    ('CAFE-100', 'Cafe soluble 100 g', 22000, 8),
    ('PAN-500', 'Pan de molde 500 g', 11000, 12),
    ('HUEVOS-12', 'Huevos por docena', 18000, 15),
    ('QUESO-500', 'Queso Paraguay 500 g', 24000, 6),
    ('YOGUR-1L', 'Yogur de frutilla 1 litro', 13000, 10),
    ('POLLO-1K', 'Pechuga de pollo 1 kg', 32000, 9),
    ('TOMATE-1K', 'Tomate 1 kg', 10000, 20),
    ('PAPA-1K', 'Papa 1 kg', 7500, 28),
    ('BANANA-1K', 'Banana 1 kg', 8000, 16),
    ('AGUA-2L', 'Agua mineral 2 litros', 5000, 50),
    ('JUGO-1L', 'Jugo de naranja 1 litro', 9500, 1),
    ('DETERGENTE-750', 'Detergente lavavajillas 750 ml', 10500, 14),
    ('JABON-90', 'Jabon de tocador 90 g', 4500, 24)
ON CONFLICT (sku) DO NOTHING;

COMMIT;

SELECT sku, nombre, precio, stock AS stock_fisico
FROM public.producto
WHERE sku IN (
    'ARROZ-1K', 'LECHE-1L', 'AZUCAR-1K', 'ACEITE-900',
    'HARINA-1K', 'FIDEO-500', 'YERBA-500', 'CAFE-100',
    'PAN-500', 'HUEVOS-12', 'QUESO-500', 'YOGUR-1L',
    'POLLO-1K', 'TOMATE-1K', 'PAPA-1K', 'BANANA-1K',
    'AGUA-2L', 'JUGO-1L', 'DETERGENTE-750', 'JABON-90'
)
ORDER BY sku;
