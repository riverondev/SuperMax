package fpuna.supermax.servicio;

import java.util.List;

import fpuna.supermax.bd.ProductoDAO;
import fpuna.supermax.entidad.Producto;

public class CatalogoServicio {

    private ProductoDAO productoDAO;

    public CatalogoServicio() {
        productoDAO = new ProductoDAO();
    }

    public List<Producto> getCatalogo(String sku) {
        if (sku == null) {
            return productoDAO.seleccionarCatalogo();
        }

        String skuLimpio = sku.trim();

        if (skuLimpio.isEmpty()) {
            throw new IllegalArgumentException("El SKU no puede estar vacío");
        }

        return productoDAO.seleccionarPorSku(skuLimpio);
    }
}