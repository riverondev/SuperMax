package fpuna.supermax.bd;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import fpuna.supermax.entidad.Reserva;
import fpuna.supermax.entidad.ReservaItem;
import fpuna.supermax.entidad.ValidacionException;

public class GestionReservaDAO {
    interface Conexion {
        Connection abrir() throws SQLException;
    }

    private final Conexion conexion;
    private final GestionReservaConsultas consultas = new GestionReservaConsultas();

    public GestionReservaDAO() {
        this(Bd::connect);
    }

    GestionReservaDAO(Conexion conexion) {
        this.conexion = conexion;
    }

    public Reserva gestionarReserva(int reservaId, String accion) throws SQLException {
        if (!"CONFIRMAR".equals(accion) && !"LIBERAR".equals(accion)) {
            throw new ValidacionException("SOLICITUD_INVALIDA", "Accion no admitida");
        }
        try (Connection conn = conexion.abrir()) {
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setAutoCommit(false);
            try {
                Reserva inicial = consultas.leerReserva(conn, reservaId, false);
                if (inicial == null) {
                    throw new ValidacionException("RESERVA_NO_EXISTE", "No existe la reserva indicada");
                }
                List<ReservaItem> items = consultas.leerItems(conn, reservaId);
                if (items.isEmpty()) {
                    throw new SQLException("Reserva sin items");
                }
                consultas.bloquearProductos(conn, reservaId, items.size());
                Reserva actual = consultas.leerReserva(conn, reservaId, true);
                if (actual == null) {
                    throw new SQLException("La reserva desaparecio durante la operacion");
                }
                actual.setItems(items);
                aplicar(conn, actual, accion);
                conn.commit();
                return actual;
            } catch (SQLException | RuntimeException error) {
                try {
                    conn.rollback();
                } catch (SQLException falloRollback) {
                    error.addSuppressed(falloRollback);
                }
                throw error;
            }
        }
    }

    private void aplicar(Connection conn, Reserva reserva, String accion) throws SQLException {
        String destino = "CONFIRMAR".equals(accion) ? "CONFIRMADA" : "LIBERADA";
        if (destino.equals(reserva.getEstado())) {
            return;
        }
        if (!"RESERVADA".equals(reserva.getEstado())) {
            if (!"CONFIRMADA".equals(reserva.getEstado())
                    && !"LIBERADA".equals(reserva.getEstado())) {
                throw new SQLException("Estado de reserva desconocido");
            }
            throw new ValidacionException("ESTADO_INVALIDO", "La accion contradice el estado de la reserva");
        }
        if ("CONFIRMAR".equals(accion)) {
            String sql = "UPDATE public.producto SET stock = stock - ? WHERE sku = ? AND stock >= ?";
            for (ReservaItem item : reserva.getItems()) {
                consultas.actualizar(conn, sql, item.getCantidad(), item.getSku(), item.getCantidad());
            }
        }
        consultas.actualizar(conn,
                "UPDATE public.reserva SET estado = ? WHERE reserva_id = ? AND estado = 'RESERVADA'",
                destino, reserva.getReservaId());
        reserva.setEstado(destino);
    }
}
