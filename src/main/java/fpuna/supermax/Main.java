package fpuna.supermax;

import fpuna.supermax.server.TCPMultiServer;

public class Main {
    public static void main(String[] args) throws Exception {
        int puerto = args.length > 0 ? Integer.parseInt(args[0]) : 5001;
        new TCPMultiServer(puerto).ejecutar();
    }
}
