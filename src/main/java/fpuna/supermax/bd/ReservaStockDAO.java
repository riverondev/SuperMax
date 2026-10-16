package fpuna.supermax.bd;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

import fpuna.supermax.entidad.Reserva;
import fpuna.supermax.entidad.ReservaItem;

public class ReservaStockDAO {

    /*
     * Recupera una reserva y sus items.
     * Devuelve null cuando el requestId no existe.
     */
    public Reserva buscarPorRequestId(Connection conn, String requestId)
            throws SQLException {

        String sql =
                "SELECT reserva_id, request_id, pedido_externo_id, "
                + "estado, creada_en, expira_en "
                + "FROM public.reserva "
                + "WHERE request_id = ?";

        Reserva reserva;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, requestId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                reserva = new Reserva();
                reserva.setReservaId(rs.getString("reserva_id"));
                reserva.setRequestId(rs.getString("request_id"));
                reserva.setPedidoExternoId(
                        rs.getString("pedido_externo_id"));
                reserva.setEstado(rs.getString("estado"));
                reserva.setCreadaEn(
                        rs.getObject("creada_en", OffsetDateTime.class));
                reserva.setExpiraEn(
                        rs.getObject("expira_en", OffsetDateTime.class));
            }
        }

        reserva.setItems(
                seleccionarItems(conn, reserva.getReservaId()));

        return reserva;
    }

    /*
     * Recupera los detalles de una reserva.
     */
    public List<ReservaItem> seleccionarItems(
            Connection conn, String reservaId) throws SQLException {

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

    /*
     * Bloquea los productos existentes en orden ascendente de SKU.
     *
     * Devuelve SKU -> stock fisico.
     * Un SKU solicitado que no aparezca en el mapa no existe.
     *
     * Requiere una transaccion con autoCommit=false.
     * Los bloqueos duran hasta commit o rollback.
     */
    public Map<String, Integer> bloquearProductos(
            Connection conn, Collection<String> skus)
            throws SQLException {

        List<String> ordenados = ordenarSkus(skus);
        Map<String, Integer> productos = new LinkedHashMap<>();

        if (ordenados.isEmpty()) {
            return productos;
        }

        String sql =
                "SELECT sku, stock "
                + "FROM public.producto "
                + "WHERE sku IN (" + crearParametros(ordenados.size()) + ") "
                + "ORDER BY sku "
                + "FOR UPDATE";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            asignarSkus(stmt, ordenados, 1);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    productos.put(
                            rs.getString("sku"),
                            rs.getInt("stock"));
                }
            }
        }

        return productos;
    }

    /*
     * Obtiene la hora real de PostgreSQL al ejecutar la sentencia.
     * El servicio debe llamarlo despues de adquirir los bloqueos.
     */
    public OffsetDateTime obtenerHoraActual(Connection conn)
            throws SQLException {

        String sql = "SELECT clock_timestamp() AS ahora";

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            if (!rs.next()) {
                throw new SQLException(
                        "No se pudo obtener la hora de PostgreSQL");
            }

            return rs.getObject("ahora", OffsetDateTime.class);
        }
    }

    /*
     * Consulta disponibilidad para todos los SKUs en una sola sentencia.
     *
     * disponible = stock fisico - reservas activas no vencidas.
     *
     * Debe ejecutarse despues de bloquearProductos(), usando la
     * misma conexion y aislamiento READ_COMMITTED.
     */
    public Map<String, Long> consultarDisponibilidad(
            Connection conn,
            Collection<String> skus,
            OffsetDateTime instanteEvaluacion) throws SQLException {

        List<String> ordenados = ordenarSkus(skus);
        Map<String, Long> disponibilidad = new LinkedHashMap<>();

        if (ordenados.isEmpty()) {
            return disponibilidad;
        }

        String sql =
                "SELECT p.sku, "
                + "p.stock::BIGINT - COALESCE(SUM( "
                + "    CASE "
                + "        WHEN r.estado = 'RESERVADA' "
                + "             AND r.expira_en > ? "
                + "        THEN ri.cantidad::BIGINT "
                + "        ELSE 0::BIGINT "
                + "    END "
                + "), 0) AS disponible "
                + "FROM public.producto p "
                + "LEFT JOIN public.reserva_item ri ON ri.sku = p.sku "
                + "LEFT JOIN public.reserva r "
                + "    ON r.reserva_id = ri.reserva_id "
                + "WHERE p.sku IN ("
                + crearParametros(ordenados.size()) + ") "
                + "GROUP BY p.sku, p.stock "
                + "ORDER BY p.sku";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, instanteEvaluacion);
            asignarSkus(stmt, ordenados, 2);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    disponibilidad.put(
                            rs.getString("sku"),
                            rs.getLong("disponible"));
                }
            }
        }

        return disponibilidad;
    }

    /*
     * Inserta cabecera y detalles en la transaccion del servicio.
     *
     * No modifica producto.stock.
     * Si falla cualquier insercion, propaga SQLException.
     * El servicio debe hacer rollback de toda la operacion.
     */
    public void insertarReserva(Connection conn, Reserva reserva)
            throws SQLException {

        if (reserva.getItems() == null
                || reserva.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "La reserva debe tener al menos un item");
        }

        insertarCabecera(conn, reserva);
        insertarItems(conn, reserva);
    }

    private void insertarCabecera(Connection conn, Reserva reserva)
            throws SQLException {

        String sql =
                "INSERT INTO public.reserva ("
                + "reserva_id, request_id, pedido_externo_id, "
                + "estado, creada_en, expira_en"
                + ") VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, reserva.getReservaId());
            stmt.setString(2, reserva.getRequestId());
            stmt.setString(3, reserva.getPedidoExternoId());
            stmt.setString(4, reserva.getEstado());
            stmt.setObject(5, reserva.getCreadaEn());
            stmt.setObject(6, reserva.getExpiraEn());

            stmt.executeUpdate();
        }
    }

    private void insertarItems(Connection conn, Reserva reserva)
            throws SQLException {

        String sql =
                "INSERT INTO public.reserva_item ("
                + "reserva_id, sku, cantidad"
                + ") VALUES (?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (ReservaItem item : reserva.getItems()) {
                stmt.setString(1, reserva.getReservaId());
                stmt.setString(2, item.getSku());
                stmt.setInt(3, item.getCantidad());

                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }

    /*
     * Reconoce exclusivamente la violacion de unicidad de request_id.
     * No confunde otros errores SQL con un reintento.
     */
    public boolean esConflictoRequestId(SQLException error) {
        SQLException actual = error;

        while (actual != null) {
            if (actual instanceof PSQLException
                    && "23505".equals(actual.getSQLState())) {

                PSQLException errorPostgres = (PSQLException) actual;
                ServerErrorMessage detalle =
                        errorPostgres.getServerErrorMessage();

                if (detalle != null
                        && "uq_reserva_request_id".equals(
                                detalle.getConstraint())) {
                    return true;
                }
            }

            actual = actual.getNextException();
        }

        return false;
    }

    /*
     * Prepara una lista sin duplicados para construir consultas.
     * El servicio debe rechazar los SKUs repetidos en la solicitud
     * antes de llamar al DAO.
     */
    private List<String> ordenarSkus(Collection<String> skus) {
        return new ArrayList<>(new TreeSet<>(skus));
    }

    /*
     * Genera exclusivamente placeholders: ?, ?, ...
     * Los valores se asignan mediante PreparedStatement.
     */
    private String crearParametros(int cantidad) {
        StringBuilder parametros = new StringBuilder();

        for (int i = 0; i < cantidad; i++) {
            if (i > 0) {
                parametros.append(", ");
            }

            parametros.append("?");
        }

        return parametros.toString();
    }

    private void asignarSkus(
            PreparedStatement stmt,
            List<String> skus,
            int primerParametro) throws SQLException {

        for (int i = 0; i < skus.size(); i++) {
            stmt.setString(primerParametro + i, skus.get(i));
        }
    }
}