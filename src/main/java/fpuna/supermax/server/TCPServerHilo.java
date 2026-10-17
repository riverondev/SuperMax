package fpuna.supermax.server;

import fpuna.supermax.servicio.GestionReservaServicio;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.sql.SQLException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import fpuna.supermax.entidad.Producto;
import fpuna.supermax.entidad.ProductoJSON;
import fpuna.supermax.servicio.CatalogoServicio;
import fpuna.supermax.servicio.ReservaStockServicio;
import fpuna.supermax.entidad.ReservaException;

public class TCPServerHilo extends Thread {
    private final Socket socket;

    public TCPServerHilo(Socket socket) {
        super("TCPServerHilo");
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                Socket cliente = socket;
                BufferedReader entrada = new BufferedReader(new InputStreamReader(
                        cliente.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(
                        cliente.getOutputStream(), StandardCharsets.UTF_8))) {
            String solicitud = entrada.readLine();
            if (solicitud != null) {
                salida.write(procesar(solicitud));
                salida.write("\n");
                salida.flush();
            }
        } catch (IOException e) {
            System.err.println("Error atendiendo cliente TCP: " + e.getMessage());
        }
    }

    private String procesar(String texto) {
        String requestId = null;

        try {
            Object valor = new JSONParser().parse(texto);
            if (!(valor instanceof JSONObject)) {
                return error(null, "SOLICITUD_INVALIDA", "La solicitud debe ser un objeto JSON");
            }

            JSONObject solicitud = (JSONObject) valor;
            String tipo = textoRequerido(solicitud, "tipo");
            requestId = textoRequerido(solicitud, "requestId");

            switch (tipo.toUpperCase()) {
                case "GET_CATALOGO":
                    CatalogoServicio s = new CatalogoServicio();
                    String sku = leerSku(solicitud);
                    List<Producto> lp = s.getCatalogo(sku);

                    System.out.println("Request atendido: " + requestId);

                    return respuestaGetCatalogo(requestId, lp);

                case "RESERVAR_STOCK":
                    return new ReservaStockServicio()
                            .ejecutar(solicitud)
                            .toJSONString();

                case "CONFIRMAR_RESERVA":
                    return new GestionReservaServicio()
                            .ejecutar(solicitud)
                            .toJSONString();

                // AGREGAR SERVICIOS ACA
                // case "SERVICIO":

                default:
                    return error(requestId, "TIPO_NO_SOPORTADO",
                            "Se admite GET_CATALOGO, RESERVAR_STOCK o CONFIRMAR_RESERVA");

            }

        } catch (SQLException e) {
            System.err.println(
                    "Error de base de datos: " + e.getMessage());

            return error(
                    requestId,
                    "ERROR_BD",
                    "No se pudo completar la operacion");

        } catch (ParseException e) {
            return error(
                    null,
                    "JSON_INVALIDO",
                    "No se pudo interpretar la solicitud");
        } catch (ReservaException e) {
            return error(requestId, e.getCodigo(), e.getMessage());

        } catch (

        IllegalArgumentException e) {
            return error(
                    requestId,
                    "CAMPO_REQUERIDO",
                    e.getMessage());
        }
    }

    private String textoRequerido(JSONObject solicitud, String campo) {
        Object valor = solicitud.get(campo);

        if (valor == null) {
            throw new ReservaException(
                    "CAMPO_REQUERIDO", "Falta el campo " + campo);
        }

        if (!(valor instanceof String)) {
            throw new ReservaException(
                    "SOLICITUD_INVALIDA",
                    "El campo " + campo + " debe ser una cadena");
        }

        String texto = (String) valor;

        if (texto.trim().isEmpty()) {
            throw new ReservaException(
                    "CAMPO_REQUERIDO", "Falta el campo " + campo);
        }

        return texto;
    }

    private String error(Object requestId, String codigo, String mensaje) {
        JSONObject respuesta = new JSONObject();
        respuesta.put("ok", false);
        respuesta.put("requestId", requestId);
        respuesta.put("codigo", codigo);
        respuesta.put("mensaje", mensaje);
        return respuesta.toJSONString();
    }

    private String leerSku(JSONObject solicitud) {
        Object valor = solicitud.get("sku");

        if (!(valor instanceof String)) {
            throw new IllegalArgumentException(
                    "El campo sku debe ser una cadena");
        }

        String sku = (String) valor;
        return sku.isEmpty() ? null : sku;
    }

    private String respuestaGetCatalogo(String requestId, List<Producto> lp) {
        // respuesta
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

        return respuesta.toJSONString();

    }
}
