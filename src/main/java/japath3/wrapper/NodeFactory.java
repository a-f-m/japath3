package japath3.wrapper;

import com.google.gson.JsonParser;

import japath3.core.Ctx;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.util.JoeUtil;

public class NodeFactory {

	static public boolean test = false;
	static Class<?> defaultClass = WJsonOrg.class;
//	static Class<?> defaultClass = WGson.class;

	public static Node w_(Object x) {
		
		return w_(x, defaultClass);
	}
	public static Node w_(Object x, Class<?> clazz) {
		
		if (clazz == WJsonOrg.class) {
			return new WJsonOrg(x instanceof String ? (test ? JoeUtil.createJoe(x.toString()) : WJsonOrg.parse(x.toString())) : x, "", null, new Ctx());
		} else if (clazz == WGson.class) {
			return new WGson(x instanceof String ? JsonParser.parseString(x.toString()) : x, "", null, new Ctx());
		} else {
			throw new JapathException("no class wrapper");
		}
	}

}
