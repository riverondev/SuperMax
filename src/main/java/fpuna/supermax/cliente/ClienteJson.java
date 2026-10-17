package fpuna.supermax.cliente;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class ClienteJson {

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "127.0.0.1";
        int puerto;
        try {
            puerto = args.length > 1 ? Integer.parseInt(args[1]) : 5001;
            if (puerto < 1 || puerto > 65535 || args.length > 2) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Uso: ClienteJson [host] [puerto entre 1 y 65535]");
            return;
        }

        System.out.println("Destino TCP: " + host + ":" + puerto);
        System.out.println("Pega un JSON en una sola linea. Escribe salir para terminar.");
        try (BufferedReader consola = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print("JSON> ");
                System.out.flush();
                String solicitud = consola.readLine();
                if (solicitud == null || "salir".equalsIgnoreCase(solicitud.trim())) {
                    break;
                }
                if (solicitud.trim().isEmpty()) {
                    continue;
                }
                try {
                    String respuesta = enviar(host, puerto, solicitud);
                    System.out.println("Respuesta:\n" + formatearJson(respuesta));
                } catch (IOException e) {
                    System.err.println("Error TCP: " + e.getMessage());
                    System.err.println("No se reenvio la solicitud automaticamente.");
                }
            }
        }
    }

    private static String formatearJson(String json) {
        try {
            new JSONParser().parse(json);
        } catch (ParseException | NumberFormatException e) {
            return json;
        }

        // Solo cambia espacios fuera de cadenas: conserva valores y orden de campos.
        StringBuilder resultado = new StringBuilder();
        int nivel = 0;
        boolean enCadena = false;
        boolean escapado = false;
        char anterior = 0;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (enCadena) {
                resultado.append(c);
                if (escapado) {
                    escapado = false;
                } else if (c == '\\') {
                    escapado = true;
                } else if (c == '"') {
                    enCadena = false;
                }
                continue;
            }
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c == '}' || c == ']') {
                nivel--;
                if (anterior != '{' && anterior != '[') {
                    nuevaLinea(resultado, nivel);
                }
            } else if (anterior == '{' || anterior == '[' || anterior == ',') {
                nuevaLinea(resultado, nivel);
            }
            resultado.append(c);
            if (c == '{' || c == '[') {
                nivel++;
            } else if (c == ':') {
                resultado.append(' ');
            } else if (c == '"') {
                enCadena = true;
            }
            anterior = c;
        }
        return resultado.toString();
    }

    private static void nuevaLinea(StringBuilder resultado, int nivel) {
        resultado.append('\n');
        for (int i = 0; i < nivel; i++) {
            resultado.append("  ");
        }
    }

    private static String enviar(String host, int puerto, String solicitud)
            throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, puerto), 5000);
            socket.setSoTimeout(10000);
            try (BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader entrada = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8))) {
                salida.write(solicitud);
                salida.newLine();
                salida.flush();
                String respuesta = entrada.readLine();
                if (respuesta == null) {
                    throw new IOException("El servidor cerro la conexion sin responder");
                }
                return respuesta;
            }
        }
    }
}
