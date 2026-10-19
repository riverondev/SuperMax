package fpuna.supermax.cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class ClienteJson {

    public static void main(String[] args) throws IOException {
        System.out.println("Destino TCP: " + ClienteTCP.HOST + ":" + ClienteTCP.PUERTO);
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
                    String respuesta = ClienteTCP.enviar( solicitud);
                    System.out.println("Respuesta:\n" + formatearJson(respuesta));
                } catch (IOException e) {
                    System.err.println("Error TCP: " + e.getMessage());
                    System.err.println("No se reenvio la solicitud automaticamente.");
                }
            }
        }
    }

    private static String formatearJson(String json) {
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

}
