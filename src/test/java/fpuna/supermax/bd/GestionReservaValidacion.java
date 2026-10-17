package fpuna.supermax.bd;

import java.sql.SQLException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import fpuna.supermax.entidad.Reserva;
import fpuna.supermax.entidad.ReservaException;
import fpuna.supermax.servicio.GestionReservaServicio;

public class GestionReservaValidacion {
    public static void main(String[] args) throws Exception {
        GestionReservaServicio sinBD = new GestionReservaServicio(new GestionReservaDAO() {
            @Override
            public Reserva gestionarReserva(int id, String accion) {
                throw new AssertionError("La entrada invalida llego al DAO");
            }
        });
        String[] invalidos = {"null", "0", "-1", "2147483648", "1.5", "1.0", "\"1\"", "true"};
        for (String valor : invalidos) {
            JSONObject respuesta = sinBD.ejecutar(json("\"r\"", valor, "\"CONFIRMAR\""));
            String codigo = "null".equals(valor) ? "CAMPO_REQUERIDO" : "SOLICITUD_INVALIDA";
            comprobar(codigo.equals(respuesta.get("codigo")), "ID invalido: " + valor);
            comprobar("r".equals(respuesta.get("requestId")), "No reflejo requestId");
        }
        for (String valor : new String[]{"null", "1", "\"\"", "\"   \""}) {
            JSONObject respuesta = sinBD.ejecutar(json(valor, "1", "\"CONFIRMAR\""));
            comprobar(Boolean.FALSE.equals(respuesta.get("ok")), "Acepto requestId invalido");
            if ("null".equals(valor) || "1".equals(valor)) {
                comprobar(respuesta.get("requestId") == null, "requestId debe ser null");
            }
        }
        JSONObject larga = json("\"r\"", "1", "\"CONFIRMAR\"");
        larga.put("requestId", new String(new char[101]).replace('\0', 'r'));
        comprobar("SOLICITUD_INVALIDA".equals(sinBD.ejecutar(larga).get("codigo")), "Acepto requestId largo");
        for (String valor : new String[]{"null", "1", "\"\"", "\"confirmar\"", "\"OTRA\""}) {
            comprobar(Boolean.FALSE.equals(sinBD.ejecutar(json("\"r\"", "1", valor)).get("ok")),
                    "Acepto accion invalida");
        }
        comprobar("CAMPO_REQUERIDO".equals(sinBD.ejecutar(null).get("codigo")), "Solicitud null");
        GestionReservaServicio exito = new GestionReservaServicio(new GestionReservaDAO() {
            @Override
            public Reserva gestionarReserva(int id, String accion) {
                comprobar(id == Integer.MAX_VALUE, "Conversion incorrecta");
                return new Reserva(id, "PED", "CONFIRMADA", null);
            }
        });
        JSONObject respuesta = exito.ejecutar(json("\"actual\"", "2147483647", "\"CONFIRMAR\""));
        comprobar(Boolean.TRUE.equals(respuesta.get("ok")) && respuesta.size() == 4, "Contrato de exito");
        comprobar("actual".equals(respuesta.get("requestId")), "requestId de respuesta incorrecto");
        GestionReservaServicio negocio = new GestionReservaServicio(new GestionReservaDAO() {
            @Override
            public Reserva gestionarReserva(int id, String accion) {
                throw new ReservaException("RESERVA_NO_EXISTE", "No existe");
            }
        });
        comprobar("RESERVA_NO_EXISTE".equals(negocio.ejecutar(json("\"r\"", "1", "\"LIBERAR\""))
                .get("codigo")), "No convierte error de negocio");
        GestionReservaServicio fallo = new GestionReservaServicio(new GestionReservaDAO() {
            @Override
            public Reserva gestionarReserva(int id, String accion) throws SQLException {
                throw new SQLException("Fallo simulado");
            }
        });
        try {
            fallo.ejecutar(json("\"r\"", "1", "\"CONFIRMAR\""));
            throw new AssertionError("Oculto el error SQL");
        } catch (SQLException esperado) {
            System.out.println("OK: validaciones v2, respuesta minima, negocio y propagacion SQL");
        }
    }

    private static JSONObject json(String request, String id, String accion) throws Exception {
        return (JSONObject) new JSONParser().parse("{\"requestId\":" + request
                + ",\"reservaId\":" + id + ",\"accion\":" + accion + "}");
    }

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
