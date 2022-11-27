package japath3.processing;

import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.lang3.StringUtils;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.json.JSONException;
import org.json.JSONObject;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.wrapper.NodeFactory;
import japath3.wrapper.WJsonOrg;

public class PegjsEngineGraal {

	Context cx = Context.create();

	private void loadStringify(Reader stringifyJs) {

		try {
			cx.eval(
					Source.newBuilder("js", stringifyJs, "str")
//					Source.newBuilder("js", new InputStreamReader(Language.class.getResourceAsStream("stringify.js")), "str")
							.build());

		} catch (Exception e) {
			throw new JapathException(e);
		}
	}
	
	public PegjsEngineGraal(Reader pegJs) {
		this(pegJs, new InputStreamReader(Language.class.getResourceAsStream("stringify.js")));
	}

	public PegjsEngineGraal(Reader pegJs, Reader stringifyJs) {

		loadStringify(stringifyJs);
		try {
			cx.eval(Source.newBuilder("js", pegJs, "pjs").build());

		} catch (Exception e) {
			throw new JapathException(e);
		}
	}

//	public PegjsEngineGraal(String js) {
//		this(new StringReader(js));
//	}
	
	public Tuple2<JSONObject, String> getAst(String text) {
		
		Tuple2<Node, Node> astNodes = getAstNodes(text, WJsonOrg.class);
		
		if (astNodes._2 != null) {
			
			JSONObject error = astNodes._2.val(); 
			
			JSONObject loc = error.getJSONObject("location");
			
			int line = loc.getJSONObject("start").getInt("line");
			int col = loc.getJSONObject("start").getInt("column");
			
			String[] split = text.split("\n");
			String frag = "..." + StringUtils.abbreviate(split[Math.min(line, split.length) - 1].substring(col > 0 ? col - 1 : 0), 20);
			
			return Tuple.of(null,
					"syntax error at line " + line
					+ ", column "
					+ col
					+ ": "
					+ "\""
					+ frag
					+ "\", "
					+ error.get("message"));
		} else {
			return Tuple.of(astNodes._1.val(), null);
		}
	}
	
	public Tuple2<Node, Node> getAstNodes(String text, Class<?> jsonClassWrapper) {
		return getAstNodes(getAstObjectsAsStrings(text), jsonClassWrapper);
	}
	
	public Tuple2<Node, Node> getAstNodes(Tuple2<String, String> t, Class<?> jsonClassWrapper) {
		
		return Tuple.of(t._1 == null ? null : NodeFactory.w_(t._1, jsonClassWrapper), //
				t._2 == null ? null : NodeFactory.w_(t._2, jsonClassWrapper));
		
	}

	private Tuple2<String, String> getAstObjectsAsStrings(String text) {

		synchronized (this) { // TODO no shared context in graal possible (->thread local)
			
			String astStr;
			try {
				cx.enter();
				Value func = cx.getBindings("js").getMember("peg$parse");
				if (func == null) throw new JapathException("fatal error eval pegjs");
				
				Value v = func.execute(text);
				astStr = v.toString();
				
			} catch (PolyglotException e) {
				
				try {
					return Tuple.of(null, e.getGuestObject().toString());
					
				} catch (JSONException e1) {
					throw new JapathException(e1);
				}
			} finally {
				cx.leave();
			}
			return Tuple.of(astStr, null);
		}
	}
}
