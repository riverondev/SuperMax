package fpuna.supermax.entidad;

public class ValidacionException extends RuntimeException {

    private final String codigo;

    public ValidacionException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}