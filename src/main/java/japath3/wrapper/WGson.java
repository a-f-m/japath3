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
	
	@Override public Node create(Object wo, Object selector, Node previousNode, Ctx ctx) {
		return new WGson(wo, selector, previousNode, ctx);
	}
	
	@Override public Object createWo(boolean array) { 
		
		return array ? new JsonArray() : new JsonObject(); }
		
	@Override
	public NodeIter get(String name) {
		
		Object o = wo instanceof JsonObject ? ((JsonObject) wo).get(name) : null; // TODO undef?
		return o == null ? empty : single(create(o, name));
	}

	@Override
	public NodeIter get(int i) {
		
		Object o = wo instanceof JsonArray && i < ((JsonArray) wo).size() ? ((JsonArray) wo).get(i) : null; // TODO undef?
		return o == null ? empty : single(create(o, i));
	}
	
	@Override public boolean exists(Object selector) {
		return wo instanceof JsonArray ? //
				(selector instanceof Integer ? ((JsonArray) wo).get((int) selector) != null : false)
				: (wo instanceof JsonObject ? ((JsonObject) wo).has(selector.toString()) : false);
	}
	
	@Override public Iterator<String> childrenSelectors() { return ((JsonObject) wo).keySet().iterator(); }

	@Override
	public NodeIter all(Object wjo) {
		
		Node prev = this;
		
		if (wjo instanceof JsonArray) {
			Iterator<JsonElement> jait = ((JsonArray) wjo).iterator();
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
		
		((JsonObject) wo).add(name, toJsonElm(o));
		return this;
	}
	
	@Override
	public Node set(int idx, Object o) {
		
		JsonArray ja = (JsonArray) wo;
		if (idx >= ja.size()) {
			
			// unfortunately, gson does not allow for extension
			
			JsonArray ja_ = new JsonArray();
			for (JsonElement je : ja) {
				ja_.add(je);
			}
			for (int i = ja.size(); i <= idx; i++) {
				ja_.add(i == idx ? toJsonElm(o) : JsonNull.INSTANCE);
			}			
			wo = ja_;
			setAncestors(null);
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
	public void remove(String name) {
		if (wo instanceof JsonObject) set(name, null);
	}
	
	@Override public Object woCopy() {
		return ((JsonElement) wo).deepCopy();
	}
	
	@Override public Object woVal() {
		
		if (wo instanceof JsonPrimitive) {
			JsonPrimitive jp = (JsonPrimitive) wo;
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
				+ (wo instanceof JsonElement ? pretty((JsonElement) wo) : wo.toString());
	}

	private String pretty(JsonElement je) throws AssertionError {
		
		
		// extra string handling due to gson inadequate formatting
		if (je.isJsonPrimitive() && ((JsonPrimitive) je).isString()) return ((JsonPrimitive) je).getAsString(); 
		if (!pretty) return je.toString();
		try {
			StringWriter stringWriter = new StringWriter();
			JsonWriter jsonWriter = new JsonWriter(stringWriter);
			jsonWriter.setLenient(true);
			jsonWriter.setIndent("   ");
			Streams.write(je, jsonWriter);
			return stringWriter.toString();
		} catch (IOException e) {
			throw new AssertionError(e);
		}
	}
	
	
}