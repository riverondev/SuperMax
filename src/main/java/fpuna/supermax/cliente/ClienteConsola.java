package fpuna.supermax.cliente;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import fpuna.supermax.entidad.Reserva;
import fpuna.supermax.entidad.ReservaItem;

public class ClienteConsola {
    private final BufferedReader consola;
    private final Map<Integer, Reserva> reservas = new LinkedHashMap<>();

    public ClienteConsola(BufferedReader consola) {
        this.consola = consola;
    }

    public static void main(String[] args) throws IOException, ParseException {
        try (BufferedReader consola = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            new ClienteConsola(consola).ejecutar();
        }
    }

    private void ejecutar() throws IOException, ParseException {
        System.out.println("Destino TCP: " + ClienteTCP.HOST + ":" + ClienteTCP.PUERTO);
        while (true) {
            System.out.println("\n===== SUPERMAX =====\n"
                    + "1. Mostrar catalogo\n2. Buscar producto por SKU\n"
                    + "3. Crear reserva\n4. Mostrar reservas de esta sesion\n"
                    + "5. Confirmar reserva\n6. Liberar reserva\n0. Salir");
            try {
                int opcion = leerEntero("Opcion: ", 0);
                switch (opcion) {
                    case 1: mostrarCatalogo(""); break;
                    case 2: mostrarCatalogo(leerTexto("SKU: ", 50)); break;
                    case 3: crearReserva(); break;
                    case 4: mostrarReservas(); break;
                    case 5: gestionarReserva("CONFIRMAR"); break;
                    case 6: gestionarReserva("LIBERAR"); break;
                    case 0: return;
                    default: System.out.println("Opcion invalida");
                }
            } catch (EOFException e) {
                return;
            } catch (IOException e) {
                System.out.println("No se pudo completar la comunicacion: " + e.getMessage());
                System.out.println("No se reenvio la solicitud. Si fue enviada, su resultado puede ser desconocido.");
            }
        }
    }

    private JSONObject solicitud(String tipo) {
        JSONObject json = new JSONObject();
        json.put("tipo", tipo);
        json.put("requestId", UUID.randomUUID().toString());
        return json;
    }

    private JSONObject enviar(JSONObject solicitud) throws IOException, ParseException {
        String texto = ClienteTCP.enviar( solicitud.toJSONString());
        JSONObject respuesta = (JSONObject) new JSONParser().parse(texto);
        if (!Boolean.TRUE.equals(respuesta.get("ok"))) {
            System.out.println("Error " + respuesta.get("codigo") + ": " + respuesta.get("mensaje"));
            return null;
        }
        return respuesta;
    }

    private boolean mostrarCatalogo(String sku) throws IOException, ParseException {
        JSONObject json = solicitud("GET_CATALOGO");
        json.put("sku", sku);
        JSONObject respuesta = enviar(json);
        if (respuesta == null) {
            return false;
        }
        JSONArray productos = (JSONArray) respuesta.get("productos");
        if (productos.isEmpty()) {
            System.out.println("No se encontraron productos.");
            return false;
        }
        System.out.printf("%-24s %-30s %12s %14s%n", "SKU", "NOMBRE", "PRECIO", "DISPONIBLE");
        for (Object valor : productos) {
            JSONObject producto = (JSONObject) valor;
            System.out.printf("%-24s %-30s %12s %14s%n", producto.get("sku"),
                    producto.get("nombre"), producto.get("precio"), producto.get("stock"));
        }
        return true;
    }

    private void crearReserva() throws IOException, ParseException {
        if (!mostrarCatalogo("")) {
            return;
        }
        String pedido = leerTexto("Pedido externo ID: ", 50);
        JSONArray itemsJson = new JSONArray();
        List<ReservaItem> items = new ArrayList<>();
        Set<String> skus = new HashSet<>();
        while (true) {
            String sku = leerLinea("SKU (Enter para terminar): ").trim();
            if (sku.isEmpty()) {
                break;
            }
            if (sku.length() > 50 || skus.contains(sku)) {
                System.out.println("El SKU debe tener hasta 50 caracteres y no estar repetido.");
                continue;
            }
            int cantidad = leerEntero("Cantidad: ", 1);
            skus.add(sku);
            JSONObject item = new JSONObject();
            item.put("sku", sku);
            item.put("cantidad", cantidad);
            itemsJson.add(item);
            items.add(new ReservaItem(null, sku, cantidad));
        }
        if (items.isEmpty()) {
            System.out.println("Reserva cancelada: no se agregaron productos.");
            return;
        }
        System.out.println("Pedido: " + pedido);
        for (ReservaItem item : items) {
            System.out.println("  " + item.getSku() + " x " + item.getCantidad());
        }
        if (!confirmar("Enviar reserva? (s/n): ")) {
            System.out.println("Reserva cancelada.");
            return;
        }
        JSONObject json = solicitud("RESERVAR_STOCK");
        json.put("pedidoExternoId", pedido);
        json.put("items", itemsJson);
        JSONObject respuesta = enviar(json);
        if (respuesta != null) {
            int id = ((Number) respuesta.get("reservaId")).intValue();
            String estado = (String) respuesta.get("estado");
            reservas.put(id, new Reserva(id, pedido, estado, items));
            System.out.println("Reserva " + id + ": " + estado);
        }
    }

    private void mostrarReservas() {
        System.out.println("Reservas de esta sesion.");
        if (reservas.isEmpty()) {
            System.out.println("No hay reservas registradas en esta sesion.");
            return;
        }
        System.out.printf("%-10s %-30s %-15s%n", "RESERVA", "PEDIDO", "ESTADO");
        for (Reserva reserva : reservas.values()) {
            System.out.printf("%-10s %-30s %-15s%n", reserva.getReservaId(),
                    reserva.getPedidoExternoId(), reserva.getEstado());
        }
    }

    private void gestionarReserva(String accion) throws IOException, ParseException {
        mostrarReservas();
        int id = leerEntero("Reserva ID (puede ser de otra sesion): ", 1);
        if (!confirmar(accion + " reserva " + id + "? (s/n): ")) {
            return;
        }
        JSONObject json = solicitud("CONFIRMAR_RESERVA");
        json.put("reservaId", id);
        json.put("accion", accion);
        JSONObject respuesta = enviar(json);
        if (respuesta != null) {
            String estado = (String) respuesta.get("estado");
            Reserva reserva = reservas.get(id);
            if (reserva != null) {
                reserva.setEstado(estado);
            } else {
                reservas.put(id, new Reserva(id, "(desconocido)", estado, null));
            }
            System.out.println("Reserva " + id + ": " + estado);
        }
    }

    private String leerLinea(String mensaje) throws IOException {
        System.out.print(mensaje);
        System.out.flush();
        String linea = consola.readLine();
        if (linea == null) {
            throw new EOFException();
        }
        return linea;
    }

    private String leerTexto(String mensaje, int maximo) throws IOException {
        while (true) {
            String texto = leerLinea(mensaje).trim();
            if (!texto.isEmpty() && texto.length() <= maximo) {
                return texto;
            }
            System.out.println("Ingrese un texto de 1 a " + maximo + " caracteres.");
        }
    }

    private int leerEntero(String mensaje, int minimo) throws IOException {
        while (true) {
            try {
                int valor = Integer.parseInt(leerLinea(mensaje).trim());
                if (valor >= minimo) {
                    return valor;
                }
            } catch (NumberFormatException e) {
                // Repetir la lectura sin salir del menu.
            }
            System.out.println("Ingrese un entero mayor o igual a " + minimo + ".");
        }
    }

    private boolean confirmar(String mensaje) throws IOException {
        while (true) {
            String valor = leerLinea(mensaje).trim();
            if ("s".equalsIgnoreCase(valor)) return true;
            if ("n".equalsIgnoreCase(valor)) return false;
            System.out.println("Ingrese s o n.");
        }
    }
}
