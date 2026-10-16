package fpuna.supermax.entidad;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class Reserva {

    private String reservaId;
    private String requestId;
    private String pedidoExternoId;
    private String estado;
    private OffsetDateTime creadaEn;
    private OffsetDateTime expiraEn;
    private List<ReservaItem> items;

    public Reserva() {
        this.items = new ArrayList<>();
    }

    public Reserva(String reservaId, String requestId, String pedidoExternoId, String estado, OffsetDateTime creadaEn, OffsetDateTime expiraEn, List<ReservaItem> items) {
        this.reservaId = reservaId;
        this.requestId = requestId;
        this.pedidoExternoId = pedidoExternoId;
        this.estado = estado;
        this.creadaEn = creadaEn;
        this.expiraEn = expiraEn;
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getReservaId() {
        return reservaId;
    }

    public void setReservaId(String reservaId) {
        this.reservaId = reservaId;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
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

    public OffsetDateTime getCreadaEn() {
        return creadaEn;
    }

    public void setCreadaEn(OffsetDateTime creadaEn) {
        this.creadaEn = creadaEn;
    }

    public OffsetDateTime getExpiraEn() {
        return expiraEn;
    }

    public void setExpiraEn(OffsetDateTime expiraEn) {
        this.expiraEn = expiraEn;
    }

    public List<ReservaItem> getItems() {
        return items;
    }

    public void setItems(List<ReservaItem> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}