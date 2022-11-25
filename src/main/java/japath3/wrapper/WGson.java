package japath3.wrapper;

import static japath3.core.Japath.empty;
import static japath3.core.Japath.single;
import static japath3.core.Node.PrimitiveType.Any;
import static japath3.core.Node.PrimitiveType.Boolean;
import static japath3.core.Node.PrimitiveType.Number;
import static japath3.core.Node.PrimitiveType.String;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

//import org.json.JSONArray;
//import org.json.JSONObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;

import japath3.core.Ctx;
import japath3.core.Japath.NodeIter;
import japath3.core.JapathException;
import japath3.core.Node;

public class WGson extends Node {
	
	public static boolean pretty = false;

	public WGson(Object wo, Object selector, Node previousNode, Ctx ctx) { super(wo, selector, previousNode, ctx); }	
	
	public static Node w_(Object x) {
		if (x instanceof String s) return NodeFactory.w_(s, WGson.class);
		return new WGson(x, "", null, new Ctx());
	}

	@Override public Node create(Object wo, Object selector, Node previousNode, Ctx ctx) {
		return new WGson(wo, selector, previousNode, ctx);
	}
	
	@Override public Object createWo(boolean array) { 
		
		return array ? new JsonArray() : new JsonObject(); }
		
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
		return wo instanceof JsonArray ja ? //
				(selector instanceof Integer ? ja.get((int) selector) != null : false)
				: (wo instanceof JsonObject jo ? jo.has(selector.toString()) : false);
	}
	
	@Override public Iterator<String> childrenSelectors() { return ((JsonObject) wo).keySet().iterator(); }

	@Override
	public NodeIter all(Object wjo) {
		
		Node prev = this;
		
		if (wjo instanceof JsonArray ja) {
			Iterator<JsonElement> jait = ja.iterator();
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
		} else if (wjo instanceof JsonObject jo) {
			Iterator<String> keys = jo.keySet().iterator();
			return new NodeIter() {

				int i = 0;
				
				@Override
				public boolean hasNext() {
					return keys.hasNext();
				}

				@Override
				public Node next() {
					String key = keys.next();
					return create(jo.get(key), key, prev, ctx).setOrder(i++);
				}
			};
		} else {
			return empty;
		}
	}
	
	@Override
	public Node set(String name, Object o) {
		
		((JsonObject) wo).add(name, toJsonElm(o));
		return this;
	}
	
	@Override
	public Node set(int idx, Object o) {
		
		JsonArray ja = (JsonArray) wo;
		
		if (idx == -1) {
			ja.add(toJsonElm(o));
			
		} else if (idx >= ja.size()) {
			
			List<JsonElement> jal = ja.asList();
			for (int i = ja.size(); i <= idx; i++) {
				jal.add(i == idx ? toJsonElm(o) : JsonNull.INSTANCE);
			}			
		} else {			
			ja.set(idx, toJsonElm(o));
		}
		return this; 
	}

	private JsonElement toJsonElm(Object o) {
		
		JsonElement je = o instanceof Number ? new JsonPrimitive((Number) o)
				: o instanceof Boolean ? new JsonPrimitive((Boolean) o)
						: o instanceof String ? new JsonPrimitive((String) o)
								: o instanceof Number ? new JsonPrimitive((Number) o)
										: o instanceof JsonElement ? (JsonElement) o : null;
		if (je == null) throw new JapathException("unknown type '" + o.getClass() + "'");
		return je;
	}
	
	@Override
	public void remove(Object selector) {
		
		if (selector instanceof String name) {
			if (wo instanceof JsonObject jo) jo.remove(name);
		} else if (selector instanceof Integer i) {
			if (wo instanceof JsonArray ja) ja.remove(i);
		}

//		if (wo instanceof JsonObject) set(name, null);
	}
	
	@Override public Object woCopy() {
		return ((JsonElement) wo).deepCopy();
	}
	
	@Override public Object woVal() {
		
		if (wo instanceof JsonPrimitive jp) {
//			JsonPrimitive jp = (JsonPrimitive) wo;
			return jp.isBoolean() ? jp.getAsBoolean()
					: jp.isNumber() ? determNumber(jp)
							: jp.isString() ? jp.getAsString()
									: null /* cannot happen */;
		} else {
			return wo;
		}
	}
	
	
	// !!! unfortunately it is necessary due to insufficient primitives handling of Gson
	private Number determNumber(JsonPrimitive jp) { 
		String asString = jp.getAsString();
		if (asString.contains(".") || asString.contains("E") || asString.contains("e")) {
			return jp.getAsDouble(); 
		} else {
			return jp.getAsInt();  
		}
	}

	@Override public Object nullWo() { return JsonNull.INSTANCE; }
	@Override public boolean isNull() { return wo == JsonNull.INSTANCE; }
		
	@Override
	public boolean type(PrimitiveType t) {

		switch (t) {
		case String: return woVal() instanceof String;
		case Number: return woVal() instanceof Number;
		case Boolean: return woVal() instanceof Boolean;
		case Any: return true;
		}
		return false;
	}
	
	@Override
	public PrimitiveType type() {

		return woVal() instanceof String ? String
				: woVal() instanceof Boolean ? Boolean //
						: woVal() instanceof Number ? Number : Any;
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
		return (wo instanceof JsonElement je ? prettyString(je, indent) : wo.toString());
	}

	public static String prettyString(JsonElement je, int indent) throws AssertionError {
		
		
		// extra string handling due to gson inadequate formatting
		if (je.isJsonPrimitive() && ((JsonPrimitive) je).isString()) return ((JsonPrimitive) je).getAsString(); 
		if (indent == 0) return je.toString();
		try {
			StringWriter stringWriter = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(stringWriter);
			jsonWriter.setLenient(true);
			jsonWriter.setIndent(StringUtils.leftPad("", indent));
			Streams.write(je, jsonWriter);
			return stringWriter.toString();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	
	public static String jsOrgPrettyString(JsonElement je, int indent) {
		
		
		String s = je.toString();
		try {
			String ret = je.isJsonArray() ? new JSONArray(s).toString(indent) : je.isJsonObject() ? new JSONObject(s).toString(indent) : null;
			if (ret == null) throw new JapathException("json array or object expected");
			return ret;
			
		} catch (Exception e) {
			throw new JapathException(e);
		}
	}
	
	public static void main(String[] args) {
		System.out.println( StringUtils.leftPad("", 0));
		System.out.println(java.lang.String.format("$4s"));
		
		new JsonArray().asList().add(0, null);
	}
	
}