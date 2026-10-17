package fpuna.supermax.servicio;

import java.sql.SQLException;
import org.json.simple.JSONObject;
import fpuna.supermax.bd.GestionReservaDAO;
import fpuna.supermax.entidad.Reserva;
import fpuna.supermax.entidad.ValidacionException;

public class GestionReservaServicio {
    private final GestionReservaDAO dao;

    public GestionReservaServicio() {
        this(new GestionReservaDAO());
    }

    public GestionReservaServicio(GestionReservaDAO dao) {
        this.dao = dao;
    }

    public JSONObject ejecutar(JSONObject solicitud) throws SQLException {
        Object valorRequest = solicitud == null ? null : solicitud.get("requestId");
        String requestId = valorRequest instanceof String ? (String) valorRequest : null;
        try {
            validarTexto(valorRequest, "requestId", 100);

            int reservaId = validarReservaId(solicitud.get("reservaId"));

            String accion = validarTexto(solicitud.get("accion"), "accion", 9);
            if (!"CONFIRMAR".equals(accion) && !"LIBERAR".equals(accion)) {
                throw new ValidacionException("SOLICITUD_INVALIDA", "accion debe ser CONFIRMAR o LIBERAR");
            }
            Reserva reserva = dao.gestionarReserva(reservaId, accion);
            JSONObject respuesta = new JSONObject();
            respuesta.put("ok", true);
            respuesta.put("requestId", requestId);
            respuesta.put("reservaId", reserva.getReservaId());
            respuesta.put("estado", reserva.getEstado());
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

    private int validarReservaId(Object valor) {
        if (valor == null || (valor instanceof String && ((String) valor).trim().isEmpty())) {
            throw new ValidacionException("CAMPO_REQUERIDO", "Falta el campo reservaId");
        }

        if (!(valor instanceof Long)) {
            throw new ValidacionException("SOLICITUD_INVALIDA", "reservaId debe ser un numero entero");
        }

        long id = (Long) valor;

        if (id <= 0 || id > Integer.MAX_VALUE) {
            throw new ValidacionException("SOLICITUD_INVALIDA", "reservaId fuera del rango permitido");
        }

        return (int) id;
    }
}
