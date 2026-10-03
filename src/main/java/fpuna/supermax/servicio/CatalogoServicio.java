package fpuna.supermax.servicio;

import java.util.List;
import java.sql.SQLException;

import fpuna.supermax.bd.ProductoDAO;
import fpuna.supermax.entidad.Producto;

public class CatalogoServicio {

    private ProductoDAO productoDAO;

    public CatalogoServicio() {
        productoDAO = new ProductoDAO();
    }

    public List<Producto> getCatalogo(String sku) throws SQLException{
        if (sku == null) {
            return productoDAO.seleccionarCatalogo();
        }

        return productoDAO.seleccionarPorSku(sku);
    }
}