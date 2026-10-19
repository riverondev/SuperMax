package fpuna.supermax.servicio;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import fpuna.supermax.bd.ReservaStockDAO;
import fpuna.supermax.entidad.ReservaItem;
import fpuna.supermax.entidad.Reserva;
import fpuna.supermax.entidad.ValidacionException;

public class ReservaStockServicio {

    private final ReservaStockDAO reservaStockDAO;

    public ReservaStockServicio() {
        this.reservaStockDAO = new ReservaStockDAO();
    }

    public JSONObject ejecutar(JSONObject solicitud) throws SQLException {
        String requestId = (String) solicitud.get("requestId");

        try {
            String pedidoExternoId = validarTexto(solicitud.get("pedidoExternoId"), "pedidoExternoId", 50);

            List<ReservaItem> items = validarItems(solicitud.get("items"));

            Reserva reserva = reservaStockDAO.crearReserva(pedidoExternoId, items);

            JSONObject respuesta = new JSONObject();
            respuesta.put("ok", true);
            respuesta.put("requestId", requestId);
            respuesta.put("reservaId", reserva.getReservaId());
            respuesta.put("estado", reserva.getEstado());
            // timestamp
            respuesta.put("timestamp", java.time.OffsetDateTime.now().toString());

            return respuesta;

        } catch (ValidacionException e) {
            return error(requestId, e.getCodigo(), e.getMessage());
        }

    }

    private List<ReservaItem> validarItems(Object valor) {
        JSONArray array = (JSONArray) valor;

        if (array.isEmpty()) {
            throw new ValidacionException("SOLICITUD_INVALIDA", "Debe incluir al menos un item");
        }

        List<ReservaItem> items = new ArrayList<>();
        Set<String> skus = new HashSet<>();

        for (Object elemento : array) {
            JSONObject objeto = (JSONObject) elemento;
            String sku = (String) objeto.get("sku");
            long cantidad = (Long) objeto.get("cantidad");

            if (cantidad <= 0) {
                throw new ValidacionException("SOLICITUD_INVALIDA", "La cantidad debe ser positiva");
            }

            if (!skus.add(sku)) {
                throw new ValidacionException("SOLICITUD_INVALIDA", "SKU repetido: " + sku);
            }

            ReservaItem item = new ReservaItem();
            item.setSku(sku);
            item.setCantidad((int) cantidad);
            items.add(item);
        }

        return items;
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

    private JSONObject error(Object requestId, String codigo, String mensaje) {
        JSONObject respuesta = new JSONObject();
        respuesta.put("ok", false);
        respuesta.put("requestId", requestId);
        respuesta.put("codigo", codigo);
        respuesta.put("mensaje", mensaje);
        return respuesta;
    }

}
