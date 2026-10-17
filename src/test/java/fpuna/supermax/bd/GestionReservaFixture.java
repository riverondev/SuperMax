package fpuna.supermax.bd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.Arrays;

final class GestionReservaFixture {
    static Connection conectar() throws SQLException {
        String url = System.getenv("GESTION_TEST_URL");
        if (url == null || !url.matches("jdbc:postgresql://[^/]+/supermax_gestion_test")) {
            throw new IllegalStateException("GESTION_TEST_URL debe apuntar a supermax_gestion_test");
        }
        return DriverManager.getConnection(url, System.getenv("GESTION_TEST_USER"),
                System.getenv("GESTION_TEST_PASSWORD"));
    }

    static GestionReservaDAO dao() {
        return new GestionReservaDAO(GestionReservaFixture::conectar);
    }

    static int crear(int... stocks) throws SQLException {
        try (Connection conn = conectar()) {
            conn.setAutoCommit(false);
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("TRUNCATE public.reserva_item, public.reserva, public.producto");
                int id;
                try (ResultSet rs = stmt.executeQuery("INSERT INTO public.reserva (pedido_externo_id) "
                        + "VALUES ('PED-TEST') RETURNING reserva_id")) {
                    rs.next();
                    id = rs.getInt(1);
                }
                for (int i = 0; i < stocks.length; i++) {
                    stmt.executeUpdate("INSERT INTO public.producto VALUES ('SKU-" + i
                            + "', 'Producto prueba', 100, " + stocks[i] + ")");
                    stmt.executeUpdate("INSERT INTO public.reserva_item VALUES (" + id
                            + ", 'SKU-" + i + "', 3)");
                }
                conn.commit();
                return id;
            } catch (SQLException error) {
                conn.rollback();
                throw error;
            }
        }
    }

    static void verificar(int id, String estado, int... stocks) throws SQLException {
        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT estado FROM public.reserva WHERE reserva_id = " + id)) {
                comprobar(rs.next() && estado.equals(rs.getString(1)), "Estado incorrecto");
            }
            int[] actuales = new int[stocks.length];
            int cantidad = 0;
            try (ResultSet rs = stmt.executeQuery("SELECT stock FROM public.producto ORDER BY sku")) {
                while (rs.next()) {
                    comprobar(cantidad < actuales.length, "Producto inesperado");
                    actuales[cantidad++] = rs.getInt(1);
                }
            }
            comprobar(cantidad == stocks.length && Arrays.equals(stocks, actuales), "Stock incorrecto");
        }
    }

    static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) {
            throw new AssertionError(mensaje);
        }
    }
}
