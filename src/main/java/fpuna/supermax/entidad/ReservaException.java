package fpuna.supermax.entidad;

public class ReservaException extends RuntimeException {

    private final String codigo;

    public ReservaException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}