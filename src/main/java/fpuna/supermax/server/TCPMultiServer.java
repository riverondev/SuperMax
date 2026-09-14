package fpuna.supermax.server;

import java.io.IOException;
import java.net.ServerSocket;

public class TCPMultiServer {
    private final int puerto;

    public TCPMultiServer(int puerto) {
        this.puerto = puerto;
    }

    public void ejecutar() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(puerto)) {
            System.out.println("SuperMax escuchando en el puerto " + puerto);
            while (true) {
                new TCPServerHilo(serverSocket.accept()).start();
            }
        }
    }
}
