package fpuna.supermax.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import fpuna.supermax.entidad.Reserva;
import fpuna.supermax.entidad.ReservaItem;

// SQL de gestion de reservas; la conexion y transaccion pertenecen al DAO.
final class GestionReservaConsultas {
    Reserva leerReserva(Connection conn, int id, boolean bloquear) throws SQLException {
        String sql = "SELECT reserva_id, pedido_externo_id, estado FROM public.reserva "
                + "WHERE reserva_id = ?" + (bloquear ? " FOR UPDATE" : "");
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Reserva(rs.getInt("reserva_id"), rs.getString("pedido_externo_id"),
                        rs.getString("estado"), null);
            }
        }
    }

    List<ReservaItem> leerItems(Connection conn, int id) throws SQLException {
        String sql = "SELECT sku, cantidad FROM public.reserva_item WHERE reserva_id = ? ORDER BY sku";
        List<ReservaItem> items = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int cantidad = rs.getInt("cantidad");
                    if (cantidad <= 0) {
                        throw new SQLException("Cantidad de reserva inconsistente");
                    }
                    items.add(new ReservaItem(id, rs.getString("sku"), cantidad));
                }
            }
        }
        return items;
    }

    void bloquearProductos(Connection conn, int id, int esperados) throws SQLException {
        String sql = "SELECT p.sku FROM public.producto p "
                + "JOIN public.reserva_item ri ON ri.sku = p.sku "
                + "WHERE ri.reserva_id = ? ORDER BY p.sku FOR UPDATE OF p";
        int bloqueados = 0;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bloqueados++;
                }
            }
        }
        if (bloqueados != esperados) {
            throw new SQLException("Los productos no coinciden con los items de la reserva");
        }
    }

    void actualizar(Connection conn, String sql, Object... valores) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < valores.length; i++) {
                stmt.setObject(i + 1, valores[i]);
            }
            if (stmt.executeUpdate() != 1) {
                throw new SQLException("Actualizacion inconsistente: se esperaba exactamente una fila");
            }
        }
    }
}
