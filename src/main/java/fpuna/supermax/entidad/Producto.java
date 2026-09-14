package fpuna.supermax.entidad;

public class Producto {

	private String sku;
	private String nombre;
	private Integer precio;
	private Integer stock;

	public Producto() {
	}

	public Producto(String sku, String nombre, Integer precio, Integer stock) {
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

	public Integer getPrecio() {
		return precio;
	}

	public void setPrecio(Integer precio) {
		this.precio = precio;
	}

	public Integer getStock() {
		return stock;
	}

	public void setStock(Integer stock) {
		this.stock = stock;
	}

}
