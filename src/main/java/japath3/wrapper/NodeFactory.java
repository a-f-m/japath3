package japath3.wrapper;

import java.io.StringReader;

import javax.json.Json;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import japath3.core.Ctx;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.util.JoeUtil;

public class NodeFactory {
	
	// const for generating empty objects; should be migrated in future
	static public Object emptyObject = new Object(); 
	static public Object emptyArray = new Object(); 

	static public boolean test = false;
	static Class<?> defaultWrapperClass = WJsonOrg.class;
//	static private Class<?> defaultWrapperClass = WGson.class;
//	static Class<?> defaultWrapperClass = WJsonB.class;

	public static Node w_(Object x) {
		
		return w_(x, defaultWrapperClass);
	}
	public static Node w_(Object x, Class<?> wclass) {
		
		if (wclass == WJsonOrg.class) {
			return new WJsonOrg(
					x instanceof String ? (test ? JoeUtil.createJoe(x.toString()) : WJsonOrg.parse(x.toString()))
							: x == emptyObject ? JoeUtil.createJoe() : x,
					"", null, new Ctx());
		} else if (wclass == WGson.class) {
			return new WGson(x instanceof String ? JsonParser.parseString(x.toString())
					: x == emptyObject ? new JsonObject() : x == emptyArray ? new JsonArray() : x, "", null, new Ctx());
		} else if (wclass == WJsonB.class) {
			return new WJsonB(x instanceof String
					? Json.createReader(new StringReader(WJsonOrg.parse(x.toString()).toString())).read()
					: x == emptyObject ? Json.createObjectBuilder().build() : x, "", null, new Ctx());
		} else {
			throw new JapathException("no class wrapper");
		}
	}
	public static void setDefaultWrapperClass(Class<?> defaultWrapperClass_) {
		defaultWrapperClass = defaultWrapperClass_;
	}

}
