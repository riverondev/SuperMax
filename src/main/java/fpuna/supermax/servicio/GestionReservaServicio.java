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
        String requestId = (String) solicitud.get("requestId");
        try {
            int reservaId = validarReservaId(solicitud.get("reservaId"));

            Object valorAccion = solicitud.get("accion");
            if (!"CONFIRMAR".equals(valorAccion) && !"LIBERAR".equals(valorAccion)) {
                throw new ValidacionException("SOLICITUD_INVALIDA", "accion debe ser CONFIRMAR o LIBERAR");
            }
            String accion = (String) valorAccion;

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

    private int validarReservaId(Object valor) {

        if (!(valor instanceof Long) || (Long) valor <= 0) {
            throw new ValidacionException("SOLICITUD_INVALIDA", "reservaId debe ser un numero entero positivo");
        }

        return ((Long) valor).intValue();
    }
}
