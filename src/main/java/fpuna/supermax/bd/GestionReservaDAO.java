package fpuna.supermax.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import fpuna.supermax.entidad.Reserva;
import fpuna.supermax.entidad.ReservaItem;
public class GestionReservaDAO {
     public Reserva buscarPorReservaId(Connection conn, String reservaId)
      throws SQLException {

        String sql =
                "SELECT reserva_id, request_id, pedido_externo_id, "
                + "estado, creada_en, expira_en "
                + "FROM public.reserva "
                + "WHERE reserva_id = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reservaId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Reserva reserva = new Reserva();
                reserva.setReservaId(rs.getString("reserva_id"));
                reserva.setRequestId(rs.getString("request_id"));
                reserva.setPedidoExternoId(
                        rs.getString("pedido_externo_id"));
                reserva.setEstado(rs.getString("estado"));
                reserva.setCreadaEn(
                        rs.getObject("creada_en", OffsetDateTime.class));
                reserva.setExpiraEn(
                        rs.getObject("expira_en", OffsetDateTime.class));

                return reserva;
            }
        }
    }

    public List<ReservaItem> seleccionarItems(Connection conn, String reservaId) 
    throws SQLException {

    String sql =
            "SELECT reserva_id, sku, cantidad "
            + "FROM public.reserva_item "
            + "WHERE reserva_id = ? "
            + "ORDER BY sku";

    List<ReservaItem> items = new ArrayList<>();

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, reservaId);

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ReservaItem item = new ReservaItem();

                item.setReservaId(rs.getString("reserva_id"));
                item.setSku(rs.getString("sku"));
                item.setCantidad(rs.getInt("cantidad"));

                items.add(item);
            }
        }
    }

    return items;
    }

    public int bloquearProductos(
        Connection conn, String reservaId) throws SQLException {

    String sql =
            "SELECT p.sku "
            + "FROM public.producto p "
            + "JOIN public.reserva_item ri ON ri.sku = p.sku "
            + "WHERE ri.reserva_id = ? "
            + "ORDER BY p.sku "
            + "FOR UPDATE OF p";

    int cantidadBloqueada = 0;

    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
        stmt.setString(1, reservaId);

        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                cantidadBloqueada++;
            }
        }
    }

    return cantidadBloqueada;
}
}
