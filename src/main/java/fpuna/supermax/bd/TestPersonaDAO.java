package fpuna.supermax.bd;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import fpuna.supermax.entidad.Persona;

public class TestPersonaDAO {

	public static void main(String args[]) throws SQLException{
		
		PersonaDAO pdao = new PersonaDAO();
		
		pdao.insertar(new Persona(200L,"Rocio", "Cabañas") );
		pdao.insertar(new Persona(201L,"Juan", "Lopez") );
		pdao.insertar(new Persona(202L,"Ana", "Iturbe") );
		pdao.insertar(new Persona(203L,"Jose", "Gomez") );
		
		pdao.actualizar(new Persona(201L,"Antonio", "Roman") );
		
		pdao.borrar(203L);
		
		//List<Persona> lista = pdao.seleccionarPorCedula(202L);
		
		List<Persona> lista = pdao.seleccionar(); 
		
		
		for (Persona p: lista){
			System.out.println(p.getCedula() + " " + p.getNombre() + " " + p.getApellido());
		}
	}
	
	
}
