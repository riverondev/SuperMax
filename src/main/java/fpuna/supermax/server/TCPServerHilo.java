package fpuna.supermax.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class TCPServerHilo extends Thread {
    private final Socket socket;

    public TCPServerHilo(Socket socket) {
        super("TCPServerHilo");
        this.socket = socket;
    }

    @Override
    public void run() {
        try (Socket cliente = socket;
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
        Object requestId = null;
        try {
            Object valor = new JSONParser().parse(texto);
            if (!(valor instanceof JSONObject)) {
                return error(null, "SOLICITUD_INVALIDA", "La solicitud debe ser un objeto JSON");
            }

            JSONObject solicitud = (JSONObject) valor;
            requestId = solicitud.get("requestId");
            String tipo = textoRequerido(solicitud, "tipo");
            String id = textoRequerido(solicitud, "requestId");
            String sucursalId = textoRequerido(solicitud, "sucursalId");
            String sku = textoRequerido(solicitud, "sku");

            if (!"GET_CATALOGO".equals(tipo)) {
                return error(id, "TIPO_NO_SOPORTADO", "Solo se admite GET_CATALOGO");
            }

            JSONArray productos = new JSONArray();
            if ("SUC-01".equals(sucursalId) && "ARROZ-1K".equals(sku)) {
                JSONObject producto = new JSONObject();
                producto.put("sku", "ARROZ-1K");
                producto.put("nombre", "Arroz 1 kg");
                producto.put("precio", 8500L);
                producto.put("disponible", true);
                producto.put("stockInformativo", 24L);
                productos.add(producto);
            }

            JSONObject respuesta = new JSONObject();
            respuesta.put("ok", true);
            respuesta.put("requestId", id);
            respuesta.put("sucursalId", sucursalId);
            respuesta.put("productos", productos);
            return respuesta.toJSONString();
        } catch (ParseException e) {
            return error(null, "JSON_INVALIDO", "No se pudo interpretar la solicitud");
        } catch (IllegalArgumentException e) {
            return error(requestId, "CAMPO_REQUERIDO", e.getMessage());
        }
    }

    private String textoRequerido(JSONObject solicitud, String campo) {
        Object valor = solicitud.get(campo);
        if (!(valor instanceof String) || ((String) valor).trim().isEmpty()) {
            throw new IllegalArgumentException("Falta el campo " + campo);
        }
        return (String) valor;
    }

    private String error(Object requestId, String codigo, String mensaje) {
        JSONObject respuesta = new JSONObject();
        respuesta.put("ok", false);
        respuesta.put("requestId", requestId);
        respuesta.put("codigo", codigo);
        respuesta.put("mensaje", mensaje);
        return respuesta.toJSONString();
    }
}
