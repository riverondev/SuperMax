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
 
	public List<Producto> seleccionarCatalogo() {
		String query = "SELECT sku, nombre, precio, stock FROM producto;";
		
		List<Producto> lista = new ArrayList<Producto>();
		
		Connection conn = null; 
        try 
        {
        	conn = Bd.connect();
        	ResultSet rs = conn.createStatement().executeQuery(query);

        	while(rs.next()) {
        		Producto p = new Producto();
        		p.setSku(rs.getString(1));
        		p.setNombre(rs.getString(2));
        		p.setPrecio(rs.getLong(3));
                p.setStock(rs.getInt(4));
        		
        		lista.add(p);
        	}
        	
        } catch (SQLException ex) {
            System.out.println("Error en la seleccion: " + ex.getMessage());
        }
        finally  {
        	try{
        		conn.close();
        	}catch(Exception ef){
        		System.out.println("No se pudo cerrar la conexion a BD: "+ ef.getMessage());
        	}
        }
		return lista;

	}
	
	public List<Producto> seleccionarPorSku(String sku) {
		String SQL = "SELECT sku, nombre, precio, stock FROM producto WHERE sku = ?;";
		
		List<Producto> lista = new ArrayList<Producto>();
		
		Connection conn = null; 
        try 
        {
        	conn = Bd.connect();
        	PreparedStatement pstmt = conn.prepareStatement(SQL);
        	pstmt.setString(1, sku);
        	
        	ResultSet rs = pstmt.executeQuery();

        	while(rs.next()) {
        		Producto p = new Producto();
        		p.setSku(rs.getString(1));
        		p.setNombre(rs.getString(2));
        		p.setPrecio(rs.getLong(3));
                p.setStock(rs.getInt(4));
        		
        		lista.add(p);
        	}
        	
        } catch (SQLException ex) {
            System.out.println("Error en la seleccion: " + ex.getMessage());
        }
        finally  {
        	try{
        		conn.close();
        	}catch(Exception ef){
        		System.out.println("No se pudo cerrar la conexion a BD: "+ ef.getMessage());
        	}
        }
		return lista;

	}

}
