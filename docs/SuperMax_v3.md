# Sistema 2: SuperMax

Comparando: Wait review

| Santiago Adrián Pérez Barreto | 5610478 |
| --- | --- |
| Juan José Augusto Riverón Meza | 5677678 |
| Lucas Enmanuel Torres Amarilla | 7014594 |

# Organización

**Empresa dueña:** SuperMax S.R.L. Su objetivo es comercializar productos de supermercado manteniendo control exacto de inventario y precios.

**Sistema:** Sistema de Comercio e Inventario SuperMax. Administra el catálogo, stock, reservas y empaquetado de pedidos. Es autónomo: posee su propia base de datos (catálogo e inventario) y no expone su estructura interna a terceros.

# Servicios

## 1. Consultar catálogo y precios

Devuelve la lista de productos disponibles y sus precios.

### **Entrada**

`requestId`, `sku`

```json
{
  "tipo": "GET_CATALOGO",
  "requestId": "req-b1-01",
  "sku": "ARROZ-1K"
}
```

### **Salida**

`ok`, `requestId`, `productos [{sku, nombre, precio, stock}]`, `timestamp` 

```json
{
  "ok": true,
  "requestId": "req-b1-01",
  "productos": [
    {
      "sku": "ARROZ-1K",
      "nombre": "Arroz 1 kg",
      "precio": 8500,
      "stock": 24
    }
  ],
  "timestamp": "2026-08-27T21:50:00-03:00"
}
```

## 2. Reservar stock

Bloquea temporalmente unidades de producto durante el proceso de compra para evitar sobreventa.

### **Entrada**

`requestId`, `pedidoExternoId`, `items [{sku, cantidad}]`

```json
{
  "tipo": "RESERVAR_STOCK",
  "requestId": "req-b2-01",
  "pedidoExternoId": "PED-101",
  "items": [
    {"sku": "ARROZ-1K", "cantidad": 2}
  ]
}
```

### **Salida**

`ok`, `requestId`, `reservaId`, `pedidoExternoId`, `estado`, `expiraEnSegundos` , `timestamp`

```json
{
  "ok": true,
  "requestId": "req-b2-01",
  "reservaId": "RES-501",
  "pedidoExternoId": "PED-101",
  "estado": "RESERVADA",
  "expiraEnSegundos": 300,
  "timestamp": "2026-08-27T21:50:00-03:00"
}
{
  "ok": false,
  "requestId": "req-b2-01",
  "codigo": "SIN_STOCK",
  "mensaje": "Stock insuficiente para ARROZ-1K (solicitado: 2, disponible: 1)"
}
```

## 3. **Confirmar o liberar reserva**

Descuenta el stock definitivamente tras el cobro o lo devuelve a la venta si se cancela la compra.

### **Entrada**

`requestId`, `reservaId`, `accion` (`CONFIRMAR` o `LIBERAR`)

```json
{
  "tipo": "CONFIRMAR_RESERVA",
  "requestId": "req-b3-01",
  "reservaId": "RES-501",
  "accion": "CONFIRMAR"
}
```

### **Salida**

`ok`, `requestId`, `reservaId`, `pedidoExternoId`, `estado` , `timestamp`

```json
{
  "ok": true,
  "requestId": "req-b3-01",
  "reservaId": "RES-501",
  "pedidoExternoId": "PED-101",
  "estado": "CONFIRMADA",
  "timestamp": "2026-08-27T21:50:00-03:00"
}
```

## 4. Consultar preparación

Informa si el paquete ya fue armado en el supermercado para ser retirado.

### **Entrada**

`requestId`, `pedidoExternoId`

```json
{
  "tipo": "GET_PREPARACION",
  "requestId": "req-b4-01",
  "pedidoExternoId": "PED-101"
}
```

### **Salida**

`ok`, `requestId`, `pedidoExternoId`, `estado` (`PREPARANDO`, `LISTO` , `CANCELADO`)  , `timestamp`

```json
{
  "ok": true,
  "requestId": "req-b4-01",
  "pedidoExternoId": "PED-101",
  "estado": "LISTO",
  "timestamp": "2026-08-27T21:50:00-03:00"
}
```

## 5. **Alerta de stock agotado**

Datagrama que notifica rápidamente cuando un producto se queda sin existencias físicas.

### **Entrada**

Agotamiento de inventario

### **Salida**

`tipo`, `sku`, `stock`, `secuencia`, `timestamp`

```json
{
  "tipo": "ALERTA_STOCK",
  "sku": "ARROZ-1K",
  "stock": 0,
  "secuencia": 42,
  "timestamp": "2026-08-27T21:50:00-03:00"
}
```