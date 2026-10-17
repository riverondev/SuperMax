package fpuna.supermax.bd;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import fpuna.supermax.entidad.ReservaException;

public class GestionReservaIntegracion {
    public static void main(String[] args) throws Exception {
        GestionReservaDAO dao = GestionReservaFixture.dao();
        int id = GestionReservaFixture.crear(10);
        dao.gestionarReserva(id, "CONFIRMAR");
        dao.gestionarReserva(id, "CONFIRMAR");
        GestionReservaFixture.verificar(id, "CONFIRMADA", 7);
        incompatible(dao, id, "LIBERAR");
        GestionReservaFixture.verificar(id, "CONFIRMADA", 7);
        id = GestionReservaFixture.crear(10);
        dao.gestionarReserva(id, "LIBERAR");
        dao.gestionarReserva(id, "LIBERAR");
        incompatible(dao, id, "CONFIRMAR");
        GestionReservaFixture.verificar(id, "LIBERADA", 10);
        try {
            dao.gestionarReserva(Integer.MAX_VALUE, "CONFIRMAR");
            throw new AssertionError("Acepto reserva inexistente");
        } catch (ReservaException error) {
            GestionReservaFixture.comprobar("RESERVA_NO_EXISTE".equals(error.getCodigo()), "Codigo incorrecto");
        }
        id = GestionReservaFixture.crear(10, 10);
        dao.gestionarReserva(id, "CONFIRMAR");
        GestionReservaFixture.verificar(id, "CONFIRMADA", 7, 7);
        id = GestionReservaFixture.crear(10, 1);
        falloSQL(dao, id);
        GestionReservaFixture.verificar(id, "RESERVADA", 10, 1);
        id = GestionReservaFixture.crear();
        falloSQL(dao, id);
        GestionReservaFixture.verificar(id, "RESERVADA");
        concurrentes(false);
        concurrentes(true);
        System.out.println("OK: confirmar, liberar, reintentos, incompatibles, inexistente, "
                + "varios productos, rollback parcial, reserva sin items y concurrencia");
    }

    private static void incompatible(GestionReservaDAO dao, int id, String accion) throws SQLException {
        try {
            dao.gestionarReserva(id, accion);
            throw new AssertionError("Acepto accion incompatible");
        } catch (ReservaException error) {
            GestionReservaFixture.comprobar("ESTADO_INVALIDO".equals(error.getCodigo()), "Codigo incorrecto");
        }
    }

    private static void falloSQL(GestionReservaDAO dao, int id) throws SQLException {
        try {
            dao.gestionarReserva(id, "CONFIRMAR");
            throw new AssertionError("No detecto inconsistencia");
        } catch (SQLException esperado) {
            // La comprobacion posterior verifica que ningun descuento quedo guardado.
        }
    }

    private static void concurrentes(boolean opuestas) throws Exception {
        int id = GestionReservaFixture.crear(10);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch listos = new CountDownLatch(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Future<String> a = executor.submit(() -> operar(id, "CONFIRMAR", listos, inicio));
            Future<String> b = executor.submit(() -> operar(id, opuestas ? "LIBERAR" : "CONFIRMAR", listos, inicio));
            GestionReservaFixture.comprobar(listos.await(5, TimeUnit.SECONDS), "Clientes no preparados");
            inicio.countDown();
            String primero = a.get(10, TimeUnit.SECONDS);
            String segundo = b.get(10, TimeUnit.SECONDS);
            String ganador = "ESTADO_INVALIDO".equals(primero) ? segundo : primero;
            if (opuestas) {
                GestionReservaFixture.comprobar("ESTADO_INVALIDO".equals(primero)
                        ^ "ESTADO_INVALIDO".equals(segundo), "Deben ganar solo una accion");
            } else {
                GestionReservaFixture.comprobar("CONFIRMADA".equals(primero)
                        && "CONFIRMADA".equals(segundo), "Ambas deben confirmar");
            }
            GestionReservaFixture.verificar(id, ganador, "CONFIRMADA".equals(ganador) ? 7 : 10);
        } finally {
            inicio.countDown();
            executor.shutdownNow();
        }
    }

    private static String operar(int id, String accion, CountDownLatch listos,
                                 CountDownLatch inicio) throws Exception {
        listos.countDown();
        inicio.await();
        try {
            return GestionReservaFixture.dao().gestionarReserva(id, accion).getEstado();
        } catch (ReservaException error) {
            return error.getCodigo();
        }
    }
}
