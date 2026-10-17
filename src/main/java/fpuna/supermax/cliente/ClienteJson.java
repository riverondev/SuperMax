package fpuna.supermax.cliente;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

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
                    System.out.println("Respuesta: " + enviar(host, puerto, solicitud));
                } catch (IOException e) {
                    System.err.println("Error TCP: " + e.getMessage());
                    System.err.println("No se reenvio la solicitud automaticamente.");
                }
            }
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
