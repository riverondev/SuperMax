package fpuna.supermax.entidad;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ProductoJSON {
    
    public static String objetoString(Producto p) {	
    	
		JSONObject obj = new JSONObject();
        obj.put("sku", p.getSku());
        obj.put("nombre", p.getNombre());
        obj.put("precio", p.getPrecio());
        obj.put("stock", p.getStock());

        return obj.toJSONString();
    }
    
    
    public static Producto stringObjeto(String str) throws Exception {
    	Producto p = new Producto();
        JSONParser parser = new JSONParser();

        JSONObject jsonObject = (JSONObject) parser.parse(str.trim());

        p.setSku((String)jsonObject.get("sku"));
        p.setNombre((String)jsonObject.get("nombre"));
        
        Number precio = (Number) jsonObject.get("precio");
        Number stock = (Number) jsonObject.get("stock");
        p.setPrecio(precio.intValue());
        p.setStock(stock.intValue());
        
        return p;
	}

}
