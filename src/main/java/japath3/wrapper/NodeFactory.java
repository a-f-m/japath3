package japath3.wrapper;

import java.io.Reader;
import java.io.StringReader;

import javax.json.Json;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import japath3.core.Ctx;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.util.JoeUtil;

public class NodeFactory {
	
	public static boolean prettyStringifying = false;
	
	// const for generating empty objects; should be migrated in future
	static public Object emptyObject = new Object(); 
	static public Object emptyArray = new Object(); 

	static private Class<?> defaultWrapperClass = WJsonOrg.class;
//	static private Class<?> defaultWrapperClass = WGson.class;
//	static Class<?> defaultWrapperClass = WJsonB.class;

	public static Node w_() {
		return w_(emptyObject);
	}
	public static Node w_(Object x) {
		
		return w_(x, defaultWrapperClass);
	}
	public static Node w_(Object x, Class<?> wclass) {
		
		try {
			if (wclass == WJsonOrg.class) {
				return new WJsonOrg(
						x instanceof Reader r ? new JSONObject(new JSONTokener(r))
								: x instanceof String ? WJsonOrg.parse(x.toString())
										: x == emptyObject ? JoeUtil.createJoe() : x == emptyArray ? new JSONArray() : x,
												"", null, new Ctx());
			} else if (wclass == WGson.class) {
				return new WGson(
						x instanceof Reader r ? JsonParser.parseReader(r)
							: x instanceof String s ? JsonParser.parseString(s)
								: x == emptyObject ? new JsonObject() : x == emptyArray ? new JsonArray() : x, "", null, new Ctx());
			} else if (wclass == WJsonB.class) {
				return new WJsonB(x instanceof String
						? Json.createReader(new StringReader(WJsonOrg.parse(x.toString()).toString())).read()
								: x == emptyObject ? Json.createObjectBuilder().build() : x, "", null, new Ctx());
			} else {
				throw new JapathException("no class wrapper");
			}
			
		} catch (Exception e) {
			throw new JapathException(e);
		}
	}
	
	public static void checkJsonWrapperClass(Node n) {

		if ((n instanceof WJsonOrg && defaultWrapperClass != WJsonOrg.class)
				|| (n instanceof WGson && defaultWrapperClass != WGson.class)
				|| (n instanceof WJsonB && defaultWrapperClass != WJsonB.class))
			throw new JapathException("uncompatible wrapper classes: '" + n.getClass() + "' <-> default: '" + defaultWrapperClass + "'");
	}
	
	public static Class<?> setDefaultWrapperClass(Class<?> defaultWrapperClass_) {
		Class<?> h = getDefaultWrapperClass();
		defaultWrapperClass = defaultWrapperClass_;
		return h;
	}
	public static Class<?> getDefaultWrapperClass() { return defaultWrapperClass; }
	
	public static void setPrettyStringifying(boolean prettyStringifying_) {
		prettyStringifying = prettyStringifying_;
	}

	public static void main(String[] args) {
		String s = """
				{
					"a":1,
					// lala
					"b":2
				}
				
				""";
		System.out.println(w_(s, WGson.class));
	}
}
