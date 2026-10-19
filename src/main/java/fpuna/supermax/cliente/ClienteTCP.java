package fpuna.supermax.cliente;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClienteTCP {
    public static final String HOST = "127.0.0.1";
    public static final int PUERTO = 5001;

    public static String enviar(String solicitud) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(HOST, PUERTO), 5000);
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

