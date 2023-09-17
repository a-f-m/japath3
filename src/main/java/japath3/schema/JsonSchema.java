package japath3.schema;

import static japath3.util.Basics.it;
import static japath3.wrapper.NodeFactory.emptyArray;
import static japath3.wrapper.NodeFactory.w_;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import io.vavr.Tuple2;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.Map;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.core.PathRepresent;

/**
 * 
 */
public class JsonSchema {
	
	private static class Defs {
		Map<String, Node> nameToNode = LinkedHashMap.empty();
		Map<String, String> nodeStrToName = LinkedHashMap.empty();
		@SuppressWarnings("unused")
		int defCnt = 0;
		synchronized Node register(boolean modular, Node structType, String sel) {
			
			if (!modular) return structType;
			
			String s = structType.woString(0);
			String name = nodeStrToName.getOrElse(s, null);
			if (name == null) {
//				name = "h" + defCnt++;
				name = sel.equals("") ? "root" : sel;
				nameToNode = nameToNode.put(name, structType);
				nodeStrToName = nodeStrToName.put(s, name);
			}
			return makeRef(name); 
		}
		private Node makeRef(String name) {
			
//			{ "$ref": "#/$defs/veggie" }
			return w_().set("$ref", "#/$defs/" + name); 
		}
	}
	
	private boolean opt;
	private boolean modular;
	private Defs defs = new Defs();
	
	public Node buildJsonTopSchema(Node n) {
		
		Node js = buildJsonSchema(n);
		Node ret = w_().set("$schema", "https://json-schema.org/draft/2020-12/schema")
				.set("$id", UUID.randomUUID().toString());;
		
		if (modular) {
			ret.set("$ref", js.val("$ref"));
		} else {
			for (Node jsn : js.all()) ret.set(jsn.selector.toString(), jsn.val()) ; 
		}
		
		if (!defs.nameToNode.isEmpty()) {
			
			Node defs_= w_();
			for (Tuple2<String, Node> def : defs.nameToNode) {
				defs_.setNode(def._1, def._2);
			}
			ret.setNode("$defs", defs_);
		}
		return ret;
	}

	private Node buildJsonSchema(Node n) {

		if (n.isStruct()) {

			Node propTypes;
			Node reqProps = null;
			Node structType = w_().set("type", "object")
					.setNode("properties", propTypes = w_());
			if (!opt) structType.setNode("required", reqProps = w_(emptyArray));

			for (Node x : it(n.all())) {
				String sel = x.selector.toString();
				if (!opt) reqProps.add(sel);
				propTypes.setNode(sel, buildJsonSchema(x));
			}

			return buildStructNode(structType, new PathRepresent().selectorPath(n));

		} else if (n.isArray()) {

			Node itemTypes;
			Node arrayTypes = w_().set("type", "array").setNode("items", w_().setNode("anyOf", itemTypes = w_(emptyArray)));

			Set<String> mem = new HashSet<>();

			for (Node x : it(n.all())) {
				Node js = buildJsonSchema(x);
				if (mem.add(js.toString())) itemTypes.addNode(js);
			}

			return arrayTypes;
		} else {
			return w_().set("type", deriveType(n));
		}

	}

	private Node buildStructNode(Node structType, String sel) {
		return defs.register(modular, structType, sel); 
	}

	private String deriveType(Node n) {

		if (n.isNull()) return "null";
		
		Object val = n.val();
		String t = val instanceof String ? "string" //
				: val instanceof Integer ? "integer" //
						: val instanceof Boolean ? "boolean" : null;
		if (t == null) throw new JapathException("primitive type '" + val.getClass() + "' not convertable");
		return t;
	}

	public JsonSchema setOpt(boolean opt) { this.opt = opt; return this; }

	public JsonSchema setModular(boolean modular) { this.modular = modular; return this; }

}
