package fpuna.supermax.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import fpuna.supermax.entidad.Reserva;
import fpuna.supermax.entidad.ValidacionException;
import fpuna.supermax.entidad.ReservaItem;

public class ReservaStockDAO {

    private Integer bloquearProducto(Connection conn, String sku) throws SQLException {

        String sql = "SELECT stock FROM producto WHERE sku = ? FOR UPDATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sku);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("stock") : null;
            }
        }
    }

    private long consultarDisponibilidad(Connection conn, String sku)
            throws SQLException {

        String sql = "SELECT p.stock - COALESCE(("
                + "SELECT SUM(ri.cantidad) FROM reserva_item ri "
                + "JOIN reserva r ON r.reserva_id = ri.reserva_id "
                + "WHERE ri.sku = p.sku AND r.estado = 'RESERVADA'"
                + "), 0) AS disponible "
                + "FROM producto p WHERE p.sku = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sku);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No se encontro el producto bloqueado");
                }

                return rs.getLong("disponible");
            }
        }
    }

    public Reserva crearReserva(String pedidoExternoId, List<ReservaItem> items) throws SQLException {

        List<ReservaItem> ordenados = new ArrayList<>(items);
        ordenados.sort(Comparator.comparing(ReservaItem::getSku));

        Reserva reserva = new Reserva(
                null, pedidoExternoId, "RESERVADA", ordenados);

        try (Connection conn = Bd.connect()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);

            try {
                for (ReservaItem item : ordenados) {
                    if (bloquearProducto(conn, item.getSku()) == null) {
                        throw new ValidacionException(
                                "PRODUCTO_NO_EXISTE",
                                "No existe el producto " + item.getSku());
                    }
                }

                for (ReservaItem item : ordenados) {
                    long disponible = consultarDisponibilidad(conn, item.getSku());

                    if (disponible < item.getCantidad()) {
                        throw new ValidacionException(
                                "SIN_STOCK",
                                "Stock insuficiente para " + item.getSku());
                    }
                }

                insertarCabecera(conn, reserva);
                insertarItems(conn, reserva);

                conn.commit();
                return reserva;
            } catch (SQLException | RuntimeException e) {
                try {
                    conn.rollback();
                } catch (SQLException errorRollback) {
                    e.addSuppressed(errorRollback);
                }
                throw e;
            }
        }
    }

    private void insertarCabecera(Connection conn, Reserva reserva)
            throws SQLException {

        String sql = "INSERT INTO reserva (pedido_externo_id) "
                + "VALUES (?) RETURNING reserva_id";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reserva.getPedidoExternoId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No se obtuvo el ID de la reserva");
                }

                reserva.setReservaId(rs.getInt("reserva_id"));
                reserva.setEstado("RESERVADA");
            }
        }
    }

    private void insertarItems(Connection conn, Reserva reserva)
            throws SQLException {

        String sql = "INSERT INTO reserva_item (reserva_id, sku, cantidad) "
                + "VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ReservaItem item : reserva.getItems()) {
                stmt.setInt(1, reserva.getReservaId());
                stmt.setString(2, item.getSku());
                stmt.setInt(3, item.getCantidad());

                stmt.executeUpdate();
            }
        }
    }
}