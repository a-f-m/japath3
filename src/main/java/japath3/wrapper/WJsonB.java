package japath3.wrapper;

import static japath3.core.Japath.empty;
import static japath3.core.Japath.single;
import static japath3.core.Node.PrimitiveType.Any;
import static japath3.core.Node.PrimitiveType.Boolean;
import static japath3.core.Node.PrimitiveType.Number;
import static japath3.core.Node.PrimitiveType.String;

import java.io.StringReader;
import java.util.Iterator;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.commons.lang3.StringUtils;
//import org.json.JSONArray;
//import org.json.JSONObject;

//import org.json.JSONArray;
//import org.json.JSONObject;

//import com.google.gson.JsonArray;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonNull;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonPrimitive;
//import com.google.gson.internal.Streams;
//import com.google.gson.stream.JsonWriter;

import japath3.core.Ctx;
import japath3.core.Japath.NodeIter;
import japath3.core.JapathException;
import japath3.core.Node;

public class WJsonB extends Node {
	
	public static boolean pretty = false;

	WJsonB(Object wo, Object selector, Node previousNode, Ctx ctx) { super(wo, selector, previousNode, ctx); }	
	
	public static Node w_(Object x) {
		return NodeFactory.w_(x, WJsonB.class);
	}

	@Override public Node create(Object wo, Object selector, Node previousNode, Ctx ctx) {
		return new WJsonB(wo, selector, previousNode, ctx);
	}
	
	@Override public Object createWo(boolean array) { 
		
		return array ? Json.createArrayBuilder().build() : Json.createObjectBuilder().build(); 
	}
		
	@Override
	public NodeIter get(String name) {
		
		Object o = wo instanceof JsonObject jo ? jo.get(name) : null; // TODO undef?
		return o == null ? empty : single(create(o, name));
	}

	@Override
	public NodeIter get(int i) {
		
		Object o = wo instanceof JsonArray ja && i >= 0 && i < ja.size() ? ja.get(i) : null; // TODO undef?
		return o == null ? empty : single(create(o, i));
	}
	
	@Override public boolean exists(Object selector) {
		
		return wo instanceof JsonValue jv
				? (jv.getValueType() == ValueType.ARRAY ? jv.asJsonArray().size() > (int) selector
						: (jv.getValueType() == ValueType.OBJECT ? jv.asJsonObject().containsKey(selector) : false))
				: false;
	}
	
	@Override public Iterator<String> childrenSelectors() { return ((JsonObject) wo).keySet().iterator(); }

	@Override
	public NodeIter all(Object wjo) {
		
		Node prev = this;
		
		if (wjo instanceof JsonArray) {
			Iterator<JsonValue> jait = ((JsonArray) wjo).iterator();
			return new NodeIter() {

				int i = 0;
				
				@Override
				public boolean hasNext() {
					return jait.hasNext();
				}

				@Override
				public Node next() {
					Node n = create(jait.next(), i, prev, ctx).setOrder(i);
					i++;
					return n;
				}
			};
		} else if (wjo instanceof JsonObject) {
			Iterator<String> keys = ((JsonObject) wjo).keySet().iterator();
			return new NodeIter() {

				int i = 0;
				
				@Override
				public boolean hasNext() {
					return keys.hasNext();
				}

				@Override
				public Node next() {
					String key = keys.next();
					return create(((JsonObject) wjo).get(key), key, prev, ctx).setOrder(i++);
				}
			};
		} else {
			return empty;
		}
	}
	
	@Override
	public Node set(String name, Object o) {
		
		wo = Json.createObjectBuilder((JsonObject) wo).add(name, toJsonVal(o)).build();
		return this;
	}
	
//	@Override
//	public Node set(int idx, Object o) {
//		
////		Json.createArrayBuilder().ad
//		
//		if (idx == -1) {
//			wo = Json.createArrayBuilder((JsonArray) wo).add(toJsonVal(o));
//		} else {
//			wo = Json.createArrayBuilder((JsonArray) wo).set(idx, toJsonVal(o)).build();
//		}
//		return this; 
//	}

	private JsonValue toJsonVal(Object o) {
		
		JsonValue ret;
		if (o instanceof Double d) {
			ret = Json.createValue(d);
		} else if (o instanceof Integer i) {
			ret = Json.createValue(i);
		} else if (o instanceof Long l) {
			ret = Json.createValue(l);
		} else if (o instanceof String s) {
			ret = Json.createValue(s);
		} else if (o instanceof Integer i) {
			return Json.createValue(i);
		} else if (o instanceof Boolean b) {
			ret = b ? JsonValue.TRUE : JsonValue.FALSE;
		} else {
			ret = null;
		}
		if (ret == null) throw new JapathException("unknown type '" + o.getClass() + "'");
		return ret;
	}
	
	@Override
	public void remove(Object selector) {
		
		if (selector instanceof String name) {
			if (wo instanceof JsonObject jo) jo.remove(name);
		} else if (selector instanceof Integer i) {
			if (wo instanceof JsonArray ja) ja.remove((int) i);
		}
	}
	
	@Override public Object woCopy() {
		
		return wo instanceof JsonObject ||wo instanceof JsonArray ? Json.createReader(new StringReader(wo.toString())).read()
				: wo;
	}
	
//	@Override public Object woVal() {
//		
////		JsonValue jv = (JsonValue) wo;
////		ValueType valueType = jv.getValueType();
////		if (wo instanceof JsonNumber n) {
////			n.
////		}
//		
//		
////		switch (valueType) {
////		case NUMBER: {
////			
////			((JsonNumber))
////			break;
////		}
////		default:
////			throw new IllegalArgumentException("Unexpected value: " + valueType);
////		}
//		
//		
//		Object ret = wo;
////		if (wo instanceof JsonNumber n) {
////		}
////		
////		if (wo instanceof JsonPrimitive) {
////			JsonPrimitive jp = (JsonPrimitive) wo;
////			return jp.isBoolean() ? jp.getAsBoolean()
////					: jp.isNumber() ? determNumber(jp)
////							: jp.isString() ? jp.getAsString()
////									: null /* cannot happen */;
////		} else {
////			return wo;
////		}
//		return ret;
//	}
	

	@Override public Object nullWo() { return JsonValue.NULL; }
	@Override public boolean isNull() { return wo == JsonValue.NULL; }
		
	@Override
	public boolean type(PrimitiveType t) {

		switch (t) {
		case String: return woVal() instanceof JsonString;
		case Number: return woVal() instanceof JsonNumber;
		case Boolean: return woVal() == JsonValue.TRUE || woVal() == JsonValue.FALSE;
		case Any: return true;
		}
		return false;
	}
	
	@Override
	public PrimitiveType type() {

		return woVal() instanceof JsonString ? String
				: woVal() == JsonValue.TRUE || woVal() == JsonValue.FALSE ? Boolean //
						: woVal() instanceof JsonNumber ? Number : Any;
	}

	@Override public boolean isLeaf() { return !(wo instanceof JsonObject || wo instanceof JsonArray); } 
	@Override public boolean isArray() { return wo instanceof JsonArray; } 
	
	@Override
	public String toString() {
		
		return "`" + selector
				+ "`->"
				+ woString(pretty ? 3 : 0);
	}
	
	@Override public String woString(int indent) { 
		return wo.toString();
	}

	@Override public Node set(int idx, Object o) { throw new UnsupportedOperationException(); }
	
	
	public static void main(String[] args) {
		System.out.println( StringUtils.leftPad("", 0));
		System.out.println(java.lang.String.format("$4s"));
		
//		JsonObject object = Json.createObjectBuilder().add("xxx", true).build();
	}
	
}