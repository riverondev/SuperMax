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

public class ReservaStockServicio {

    private final ReservaStockDAO reservaStockDAO;

    public ReservaStockServicio() {
        this.reservaStockDAO = new ReservaStockDAO();
    }

    public JSONObject ejecutar(JSONObject solicitud) throws SQLException {
        Object valorRequestId = solicitud.get("requestId");

        if (valorRequestId == null) {
            return error(null, "CAMPO_REQUERIDO", "Falta el campo requestId");
        }

        if (!(valorRequestId instanceof String)) {
            return error(null, "SOLICITUD_INVALIDA", "El campo requestId debe ser una cadena");
        }

        String requestId = (String) valorRequestId;

        if (requestId.trim().isEmpty()) {
            return error(requestId, "CAMPO_REQUERIDO", "El campo requestId no puede estar vacio");
        }

        if (requestId.length() > 100) {
            return error(requestId, "SOLICITUD_INVALIDA", "El campo requestId no puede superar 100 caracteres");
        }

        Object valorPedido = solicitud.get("pedidoExternoId");

        if (valorRequestId == null) {
            return error(null, "CAMPO_REQUERIDO", "Falta el campo pedidoExternoId");
        }

        if (!(valorRequestId instanceof String)) {
            return error(null, "SOLICITUD_INVALIDA", "El campo pedidoExternoId debe ser una cadena");
        }

        String pedidoExternoId = (String) valorPedido;

        if (requestId.trim().isEmpty()) {
            return error(requestId, "CAMPO_REQUERIDO", "El campo pedidoExternoId no puede estar vacio");
        }

        if (requestId.length() > 50) {
            return error(requestId, "SOLICITUD_INVALIDA", "El campo pedidoExternoId no puede superar 50 caracteres");
        }

        List<ReservaItem> items;

        try {
            items = validarItems(solicitud.get("items"));
        } catch (IllegalArgumentException e) {
            return error(requestId, "SOLICITUD_INVALIDA", e.getMessage());
        }
    }

    private List<ReservaItem> validarItems(Object valor) {
        if (!(valor instanceof JSONArray)) {
            throw new IllegalArgumentException(
                    "El campo items debe ser un array");
        }

        JSONArray array = (JSONArray) valor;

        if (array.isEmpty()) {
            throw new IllegalArgumentException(
                    "La solicitud debe incluir al menos un item");
        }

        List<ReservaItem> items = new ArrayList<>();
        Set<String> skus = new HashSet<>();

        for (Object elemento : array) {
            if (!(elemento instanceof JSONObject)) {
                throw new IllegalArgumentException(
                        "Cada item debe ser un objeto JSON");
            }

            JSONObject objeto = (JSONObject) elemento;

            Object valorSku = objeto.get("sku");

            if (!(valorSku instanceof String)) {
                throw new IllegalArgumentException(
                        "El sku de cada item debe ser una cadena");
            }

            String sku = (String) valorSku;

            if (sku.trim().isEmpty() || sku.length() > 50) {
                throw new IllegalArgumentException(
                        "El sku debe tener entre 1 y 50 caracteres");
            }

            if (!skus.add(sku)) {
                throw new IllegalArgumentException(
                        "SKU repetido: " + sku);
            }

            Object valorCantidad = objeto.get("cantidad");

            if (!(valorCantidad instanceof Long)) {
                throw new IllegalArgumentException(
                        "La cantidad debe ser un numero entero");
            }

            long cantidad = (Long) valorCantidad;

            if (cantidad <= 0 || cantidad > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("La cantidad debe estar entre 1 y " + Integer.MAX_VALUE);
            }

            ReservaItem item = new ReservaItem();
            item.setSku(sku);
            item.setCantidad((int) cantidad);

            items.add(item);
        }

        return items;
    }

    private JSONObject error(
            String requestId, String codigo, String mensaje) {
        JSONObject respuesta = new JSONObject();

        respuesta.put("ok", false);
        respuesta.put("requestId", requestId);
        respuesta.put("codigo", codigo);
        respuesta.put("mensaje", mensaje);

        return respuesta;
    }
}
