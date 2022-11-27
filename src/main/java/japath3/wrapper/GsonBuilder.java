package japath3.wrapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import japath3.core.JapathException;
import japath3.core.Node;

@Deprecated // deferred, use apath nodes instead
public class GsonBuilder {

	public JsonElement je;

	public GsonBuilder() {
		this(new JsonObject());
	}

	public GsonBuilder(JsonElement je) {
		if (!(je.isJsonObject() || je.isJsonArray()))
			throw new JapathException("json object or array expected (passed '" + je + "')");
		this.je = je;
	}
	
	public GsonBuilder add(String property, GsonBuilder gb) {
		return add(property, gb.je);
	}

	public GsonBuilder add(String property, JsonElement value) {

		if (je instanceof JsonObject jo) {
			jo.add(property, value);
		} else {
			throw new JapathException("operation on json object '" + je + "' not allowed");
		}
		return this;
	}
	
	public GsonBuilder add(GsonBuilder gb) {
		return add(gb.je);
	}

	public GsonBuilder add(JsonElement value) {
		
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

	public static GsonBuilder gobject() {
		return new GsonBuilder();
	}

	public static GsonBuilder garray() {
		return new GsonBuilder(new JsonArray());
	}
	
	public static void main(String[] args) {

//		GsonBuilder g = new GsonBuilder()

		GsonBuilder g = gobject()
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
	}

}
