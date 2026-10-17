package fpuna.supermax.entidad;

import java.util.ArrayList;
import java.util.List;

public class Reserva {

    private Integer reservaId;
    private String pedidoExternoId;
    private String estado;
    private List<ReservaItem> items;

    public Reserva() {
        this.items = new ArrayList<>();
    }

    public Reserva(Integer reservaId, String pedidoExternoId,
                   String estado, List<ReservaItem> items) {
        this.reservaId = reservaId;
        this.pedidoExternoId = pedidoExternoId;
        this.estado = estado;
        this.items = items != null ? items : new ArrayList<>();
    }

    public Integer getReservaId() {
        return reservaId;
    }

    public void setReservaId(Integer reservaId) {
        this.reservaId = reservaId;
    }

    public String getPedidoExternoId() {
        return pedidoExternoId;
    }

    public void setPedidoExternoId(String pedidoExternoId) {
        this.pedidoExternoId = pedidoExternoId;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<ReservaItem> getItems() {
        return items;
    }

    public void setItems(List<ReservaItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
