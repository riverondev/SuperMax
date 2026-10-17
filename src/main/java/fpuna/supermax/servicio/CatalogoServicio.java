package fpuna.supermax.servicio;

import java.util.List;
import java.sql.SQLException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import fpuna.supermax.bd.ProductoDAO;
import fpuna.supermax.entidad.Producto;
import fpuna.supermax.entidad.ProductoJSON;
import fpuna.supermax.entidad.Reserva;
import fpuna.supermax.entidad.ValidacionException;

public class CatalogoServicio {

    private ProductoDAO productoDAO;

    public CatalogoServicio() {
        productoDAO = new ProductoDAO();
    }

    public CatalogoServicio(ProductoDAO productoDAO) {
        this.productoDAO = productoDAO;
    }

    public JSONObject ejecutar(JSONObject solicitud) throws SQLException {
        Object valorRequest = solicitud == null ? null : solicitud.get("requestId");
        String requestId = valorRequest instanceof String ? (String) valorRequest : null;
        try {
            validarTexto(valorRequest, "requestId", 100);

            String sku = validarSku(solicitud);
            List<Producto> lp = sku == null ? productoDAO.seleccionarCatalogo() : productoDAO.seleccionarPorSku(sku);

            JSONObject respuesta = new JSONObject();
            respuesta.put("ok", true);
            respuesta.put("requestId", requestId);

            JSONArray list = new JSONArray();
            for (Producto producto : lp) {
                list.add(ProductoJSON.objetoJson(producto));
            }
            respuesta.put("productos", list);

            // timestamp
            respuesta.put("timestamp", java.time.OffsetDateTime.now().toString());

            return respuesta;

        } catch (ValidacionException error) {
            JSONObject respuesta = new JSONObject();
            respuesta.put("ok", false);
            respuesta.put("requestId", requestId);
            respuesta.put("codigo", error.getCodigo());
            respuesta.put("mensaje", error.getMessage());
            return respuesta;
        }

    }

    private String validarTexto(Object valor, String campo, int maximo) {
        if (valor == null || (valor instanceof String && ((String) valor).trim().isEmpty())) {
            throw new ValidacionException("CAMPO_REQUERIDO", "Falta el campo " + campo);
        }
        if (!(valor instanceof String) || ((String) valor).length() > maximo) {
            throw new ValidacionException("SOLICITUD_INVALIDA", "Tipo o longitud invalidos para " + campo);
        }
        return (String) valor;
    }

    private String validarSku(JSONObject solicitud) {
        Object valor = solicitud.get("sku");

        if (!(valor instanceof String)) {
            throw new ValidacionException("SOLICITUD_INVALIDA", "El campo sku debe ser una cadena");
        }

        String sku = (String) valor;
        return sku.isEmpty() ? null : sku;
    }

}