package japath3.processing;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import io.vavr.collection.Map;
import japath3.core.Japath.ParametricExprDef;
import japath3.core.JapathException;
import japath3.processing.Language.Env;

public class StandardDefs {

	public static Map<String, ParametricExprDef> globalDefs;
	
	static {
		extracted();
	}
	
	private static void extracted() {
		try {
			if (globalDefs == null) {
				Env env = new Env();
				Language.parse(env, IOUtils.toString(StandardDefs.class.getResourceAsStream("std-defs.ap"), "utf-8"), false);
				globalDefs = env.defs;
			}
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}

}
