package fpuna.supermax.entidad;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PersonaJSON {

    public static void main(String[] args) throws Exception {
    	PersonaJSON representacion = new PersonaJSON();
    	
    	System.out.println("Ejemplo de uso 1: pasar de objeto a string");
    	Persona p = new Persona();
    	p.setNombre("Maria");
    	p.setApellido("Gomez");
    	p.setCedula(123456L);
    	p.getAsignaturas().add("Algoritmos y Estructuras de Datos 2");
    	p.getAsignaturas().add("Quimica");
    	p.getAsignaturas().add("Ingles");
    	
    	String r1 = representacion.objetoString(p);
    	System.out.println(r1);
    	
    	
    	System.out.println("\n*************************************************************************");
    	System.out.println("\nEjemplo de uso 2: pasar de string a objeto");
    	String un_string = "{\"cedula\":123123,\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"asignaturas\":[\"Sistemas Distribuidos\",\"Fisica\",\"Inteligencia Artificial\"]}";
    	
    	Persona r2 = representacion.stringObjeto(un_string);
    	System.out.println(r2.nombre + " " + r2.apellido + " " +r2.cedula );
        for(String temp: r2.getAsignaturas()){
        	System.out.println(temp);
        }
    }
    
    public static String objetoString(Persona p) {	
    	
		JSONObject obj = new JSONObject();
        obj.put("cedula", p.getCedula());
        obj.put("nombre", p.getNombre());
        obj.put("apellido", p.getApellido());

        JSONArray list = new JSONArray();
        
        for(String temp: p.getAsignaturas()){
        	list.add(temp);
        }
       // if(list.size() > 0) {
        	obj.put("asignaturas", list);
        //}
        

        return obj.toJSONString();
    }
    
    
    public static Persona stringObjeto(String str) throws Exception {
    	Persona p = new Persona();
        JSONParser parser = new JSONParser();

        Object obj = parser.parse(str.trim());
        JSONObject jsonObject = (JSONObject) obj;

        Long cedula = (Long) jsonObject.get("cedula");
        p.setCedula(cedula);
        p.setNombre((String)jsonObject.get("nombre"));
        p.setApellido((String)jsonObject.get("apellido"));
        
        JSONArray msg = (JSONArray) jsonObject.get("asignaturas");
        Iterator<String> iterator = msg.iterator();
        while (iterator.hasNext()) {
        	p.getAsignaturas().add(iterator.next());
        }
        return p;
	}

}
