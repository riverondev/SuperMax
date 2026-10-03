package fpuna.supermax.bd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import fpuna.supermax.entidad.Producto;

public class ProductoDAO {

	public List<Producto> seleccionarCatalogo() throws SQLException {
    String query = "SELECT sku, nombre, precio, stock FROM producto;";
    List<Producto> lista = new ArrayList<>();

    Connection conn = null;

    try {
        conn = Bd.connect();
        ResultSet rs = conn.createStatement().executeQuery(query);

        while (rs.next()) {
            Producto p = new Producto();
            p.setSku(rs.getString(1));
            p.setNombre(rs.getString(2));
            p.setPrecio(rs.getLong(3));
            p.setStock(rs.getInt(4));

            lista.add(p);
        }

        return lista;
    } finally {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println(
                        "No se pudo cerrar la conexión a BD: "
                                + e.getMessage());
            }
        }
    }
}

public List<Producto> seleccionarPorSku(String sku) throws SQLException {
    String query =
            "SELECT sku, nombre, precio, stock FROM producto WHERE sku = ?;";
    List<Producto> lista = new ArrayList<>();

    Connection conn = null;

    try {
        conn = Bd.connect();
        PreparedStatement pstmt = conn.prepareStatement(query);
        pstmt.setString(1, sku);

        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            Producto p = new Producto();
            p.setSku(rs.getString(1));
            p.setNombre(rs.getString(2));
            p.setPrecio(rs.getLong(3));
            p.setStock(rs.getInt(4));

            lista.add(p);
        }

        return lista;
    } finally {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                System.err.println(
                        "No se pudo cerrar la conexión a BD: "
                                + e.getMessage());
            }
        }
    }
}

}
