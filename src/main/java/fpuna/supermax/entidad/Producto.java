package fpuna.supermax.entidad;

public class Producto {

	private String sku;
	private String nombre;
	private Long precio;
	private Integer stock;

	public Producto() {
	}

	public Producto(String sku, String nombre, Long precio, Integer stock) {
		this.sku = sku;
		this.nombre = nombre;
		this.precio = precio;
		this.stock = stock;
	}

	public String getSku() {
		return sku;
	}

	public void setSku(String sku) {
		this.sku = sku;
	}

	public String getNombre() {
		return nombre;
	}

	public void setNombre(String nombre) {
		this.nombre = nombre;
	}

	public Long getPrecio() {
		return precio;
	}

	public void setPrecio(Long precio) {
		this.precio = precio;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

}
