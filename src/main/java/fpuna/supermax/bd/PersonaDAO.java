package fpuna.supermax.bd;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import fpuna.supermax.entidad.Persona;

public class PersonaDAO {
 
	/**
	 * 
	 * @param condiciones 
	 * @return
	 */
	public List<Persona> seleccionar() {
		String query = "SELECT cedula, nombre, apellido FROM persona ";
		
		List<Persona> lista = new ArrayList<Persona>();
		
		Connection conn = null; 
        try 
        {
        	conn = Bd.connect();
        	ResultSet rs = conn.createStatement().executeQuery(query);

        	while(rs.next()) {
        		Persona p = new Persona();
        		p.setCedula(rs.getLong(1));
        		p.setNombre(rs.getString(2));
        		p.setApellido(rs.getString(3));
        		
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
	
	public List<Persona> seleccionarPorCedula(long cedula) {
		String SQL = "SELECT cedula, nombre, apellido FROM persona WHERE cedula = ? ";
		
		List<Persona> lista = new ArrayList<Persona>();
		
		Connection conn = null; 
        try 
        {
        	conn = Bd.connect();
        	PreparedStatement pstmt = conn.prepareStatement(SQL);
        	pstmt.setLong(1, cedula);
        	
        	ResultSet rs = pstmt.executeQuery();

        	while(rs.next()) {
        		Persona p = new Persona();
        		p.setCedula(rs.getLong(1));
        		p.setNombre(rs.getString(2));
        		p.setApellido(rs.getString(3));
        		
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
	
    public long insertar(Persona p) throws SQLException {

        String SQL = "INSERT INTO persona(cedula, nombre,apellido) "
                + "VALUES(?,?,?)";
 
        long id = 0;
        Connection conn = null;
        
        try 
        {
        	conn = Bd.connect();
        	PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
            pstmt.setLong(1, p.getCedula());
            pstmt.setString(2, p.getNombre());
            pstmt.setString(3, p.getApellido());
 
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows 
            if (affectedRows > 0) {
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getLong(1);
                    }
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            System.out.println("Error en la insercion: " + ex.getMessage());
        }
        finally  {
        	try{
        		conn.close();
        	}catch(Exception ef){
        		System.out.println("No se pudo cerrar la conexion a BD: "+ ef.getMessage());
        	}
        }
        	
        return id;
    	
    	
    }
	

    public long actualizar(Persona p) throws SQLException {

        String SQL = "UPDATE persona SET nombre = ? , apellido = ? WHERE cedula = ? ";
 
        long id = 0;
        Connection conn = null;
        
        try 
        {
        	conn = Bd.connect();
        	PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, p.getNombre());
            pstmt.setString(2, p.getApellido());
            pstmt.setLong(3, p.getCedula());
 
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows 
            if (affectedRows > 0) {
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getLong(1);
                    }
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            System.out.println("Error en la actualizacion: " + ex.getMessage());
        }
        finally  {
        	try{
        		conn.close();
        	}catch(Exception ef){
        		System.out.println("No se pudo cerrar la conexion a BD: "+ ef.getMessage());
        	}
        }
        return id;
    }
    
    public long borrar(long cedula) throws SQLException {

        String SQL = "DELETE FROM persona WHERE cedula = ? ";
 
        long id = 0;
        Connection conn = null;
        
        try 
        {
        	conn = Bd.connect();
        	PreparedStatement pstmt = conn.prepareStatement(SQL);
            pstmt.setLong(1, cedula);
 
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows 
            if (affectedRows > 0) {
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        id = rs.getLong(1);
                    }
                } catch (SQLException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        } catch (SQLException ex) {
            System.out.println("Error en la eliminación: " + ex.getMessage());
        }
        finally  {
        	try{
        		conn.close();
        	}catch(Exception ef){
        		System.out.println("No se pudo cerrar la conexion a BD: "+ ef.getMessage());
        	}
        }
        return id;
    }
    

}
