package japath3.wrapper;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import io.vavr.collection.List;
import io.vavr.gson.VavrGson;
import japath3.core.JapathException;
import japath3.core.Node;

public class GsonConstructor {
	
	public static Gson gson;

	public JsonElement je;
	
	static {
		GsonBuilder builder = new GsonBuilder();
		VavrGson.registerAll(builder);
		gson = builder.create();	
	}

	public GsonConstructor() {
		this(new JsonObject());
	}

	public GsonConstructor(JsonElement je) {
		if (!(je.isJsonObject() || je.isJsonArray()))
			throw new JapathException("json object or array expected (passed '" + je + "')");
		this.je = je;
	}
	
	public GsonConstructor add(String property, GsonConstructor gc) {
		return add(property, gc.je);
	}

	public GsonConstructor add(String property, JsonElement value) {

		if (je instanceof JsonObject jo) {
			jo.add(property, value);
		} else {
			throw new JapathException("operation on json object '" + je + "' not allowed");
		}
		return this;
	}
	
	public GsonConstructor add(GsonConstructor gc) {
		return add(gc.je);
	}

	public GsonConstructor add(JsonElement value) {
		
		if (je instanceof JsonArray ja) {
			ja.add(value);
		} else {
			throw new JapathException("operation on json array '" + je + "' not allowed");
		}
		return this;
	}
	
	public static JsonPrimitive gval(Object o) {

		if (o instanceof Number n) {
			return new JsonPrimitive(n);
		}
		if (o instanceof Boolean b) {
			return new JsonPrimitive(b);
		}
		if (o instanceof String s) {
			return new JsonPrimitive(s);
		} else {
			throw new JapathException("'" + o + "' must be a Number, Boolen, or String");
		}
	}

	public static GsonConstructor gobject() {
		return new GsonConstructor();
	}

	public static GsonConstructor garray() {
		return new GsonConstructor(new JsonArray());
	}
	
	public static <T> T getVal(JsonObject je, String name) {
		
		T ret = (T) WGson.woValHelper(je.get(name));
		if (ret == null) {
			throw new JapathException("propety '" + name + "' not found in \n" + je);
		}
		return ret;
	}
	
	public static void main(String[] args) {

		GsonConstructor g = gobject()
				.add("xxx", gval("..."))
				.add("yyy", gval(99)) 
				.add("nested", 
						garray()
							.add(gval(88))
							.add(gobject().add("lolo", gval(99))));

		System.out.println(g.je.toString());
		
		Node n = NodeFactory.w_(new JsonObject(), WGson.class);
		
		n //
				.set("xxx", "...") //
				.set("yyy", 99) //
				.set("nested", // 
						n.freshNode(true) //
							.add(88)
							.add(
									n.freshNode().set("lolo", 99).val()
									)
							.val()
							);
		
		System.out.println(n);
		
		List<List<Integer>> l = List.of(List.of(1));

		JsonElement jsonTree = gson.toJsonTree(l);

//		  gson.toJson(l, new AppendableOutputStream<Appendable>(null));
//		FileWriter fw = new FileWriter("xxx.txt");
//		gson.toJson(l, fw);
//		fw.flush();
//		fw.close();
		
		Type type = new TypeToken<List<List<Integer>>>(){}.getType();
		l = gson.fromJson(jsonTree, type);
		
		System.out.println(l);

	}

}
