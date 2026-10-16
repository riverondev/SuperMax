package fpuna.supermax.entidad;

public class ReservaItem {

    private String reservaId;
    private String sku;
    private Integer cantidad;

    public ReservaItem() {
    }

    public ReservaItem(String reservaId, String sku, Integer cantidad) {
        this.reservaId = reservaId;
        this.sku = sku;
        this.cantidad = cantidad;
    }

    public String getReservaId() {
        return reservaId;
    }

    public void setReservaId(String reservaId) {
        this.reservaId = reservaId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }
}