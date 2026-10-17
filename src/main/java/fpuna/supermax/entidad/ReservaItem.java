package fpuna.supermax.entidad;

public class ReservaItem {

    private Integer reservaId;
    private String sku;
    private Integer cantidad;

    public ReservaItem() {
    }

    public ReservaItem(Integer reservaId, String sku, Integer cantidad) {
        this.reservaId = reservaId;
        this.sku = sku;
        this.cantidad = cantidad;
    }

    public Integer getReservaId() {
        return reservaId;
    }

    public void setReservaId(Integer reservaId) {
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
