package japath3.schema;

import static japath3.util.Basics.it;
import static japath3.wrapper.NodeFactory.emptyArray;
import static japath3.wrapper.NodeFactory.w_;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;

import dev.harrel.jsonschema.Error;
import dev.harrel.jsonschema.Validator.Result;
import dev.harrel.jsonschema.ValidatorFactory;
import dev.harrel.jsonschema.providers.GsonNode;
import io.vavr.Tuple2;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.Map;
import io.vavr.control.Option;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.core.PathRepresent;
import japath3.util.Basics;
import japath3.wrapper.NodeFactory;

/**
 * 
 */
public class JsonSchemaProcessing {
	
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
	private boolean complete;
	private boolean modular;
	private boolean onlyTopModular = true;
	private Defs defs = new Defs();
	
	private Node schemaBundle;
	private Node prototypeBundle;
	private static GsonNode.Factory gsonNodeFactory = new GsonNode.Factory();
	private static ValidatorFactory validator = new ValidatorFactory().withJsonNodeFactory(gsonNodeFactory);
	
	public Node buildJsonTopSchema(Node n) {
		
		Node root = n.node("$defs").detach();
		Node js = buildJsonSchema(root, root, 0);
		Node ret = w_().set("$schema", "https://json-schema.org/draft/2020-12/schema")
//				.set("$id", UUID.randomUUID().toString())
				;
		
		String topRef = js.val("$ref", null);
		if (modular && topRef != null) {
			ret.set("$ref", topRef);
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

	public Node buildJsonSchema(Node n, Node root, int level) {

		if (n.isStruct()) {

			Node propTypes;
			Node reqProps = null;
			Node structType = w_().set("type", "object")
					.setNode("properties", propTypes = w_());
			if (!opt) structType.setNode("required", reqProps = w_(emptyArray));
			if (complete) structType.set("additionalProperties", false);
			
			String use = n.val("$ref", null);
			if (use != null) {
				if (!modular) throw new JapathException("$ref only allowed in modular mode");
				if (n.length() > 1) throw new JapathException("$ref must be the only property");
				return n;
			}
			
			for (Node x : it(n.all())) {
				String sel = x.selector.toString();
				if (sel.matches("^\\$proto\\:.*")) continue;
				if (!opt) reqProps.add(sel);
				propTypes.setNode(sel, buildJsonSchema(x, root, level + 1));
			}

			// handle proto spec
			Node protoSpec = n.node("$proto:json-schema");
			if (protoSpec != Node.nil) {
				for (Node spec : protoSpec.all()) structType.set(spec.selector.toString(), spec.val());
			}
			//
			return buildStructNode(structType, new PathRepresent().selectorPath(n), level);

		} else if (n.isArray()) {

			Node itemTypes;
			Node arrayTypes = w_().set("type", "array").setNode("items", w_().setNode("anyOf", itemTypes = w_(emptyArray)));

			Set<String> mem = new HashSet<>();

			for (Node x : it(n.all())) {
				Node js = buildJsonSchema(x, root, level + 1);
				if (mem.add(js.toString())) itemTypes.addNode(js);
			}

			return arrayTypes;
		} else {
			return w_().set("type", deriveType(n)).set("example", n.val());
		}

	}

	private Node buildStructNode(Node structType, String sel, int level) {
		boolean m = modular && (onlyTopModular ? level <= 1 : true);
		return defs.register(m, structType, sel); 
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
	
	public Option<List<Error>> validate(Node instance, String name) {
		
		
		Node defs_ = schemaBundle.node("$defs");
		if (defs_.node(name) == Node.nil) throw new JapathException("subSchema '" + name + "' not found");

		Node alignedToSubSchema = NodeFactory.w_().set("$ref", "#/$defs/" + name).set("$defs", defs_.val());
		
		Result result =
				validator.validate(gsonNodeFactory.wrap(alignedToSubSchema.woVal()), gsonNodeFactory.wrap(instance.woVal()));
		
		return result.isValid() ? Option.none() :  Option.of(result.getErrors());
	}
	
	public String errorText(List<Error> errors) {
		
		return errors.stream().map(
				error -> {return error.getInstanceLocation() + ": " + error.getError();}
				).collect(Collectors.joining("\n"));
	}
	
	public JsonSchemaProcessing usePrototypeBundle(Node prototypeBundle) {
		
		this.prototypeBundle = prototypeBundle;
		setSchemaBundle(buildJsonTopSchema(prototypeBundle));
		return this;
	}
	
	private void resolvePrototypeBundle_trav(Object current, JSONObject root, int level) { 
		
		if (current instanceof JSONObject joCurrent) {
			
			// post process
			List<String> props = Basics.streamIt(joCurrent.keys()).collect(Collectors.toList());
			props.forEach(prop -> {
				if (prop.startsWith("$proto:")) {
					joCurrent.remove(prop);
				} else {
					resolvePrototypeBundle_trav(joCurrent.get(prop), root, level + 1);
				}
			});
			//
			
			String ref = joCurrent.optString("$ref", null);
			if (ref != null) {
				joCurrent.remove("$ref");
				if (ref.matches("^(\\/|\\#\\/).*")) {
					
					if (new JSONPointer(ref).queryFrom(root) instanceof JSONObject joRef) {
						if (joRef != null) {
							Iterator<String> keys = joRef.keys();
							while (keys.hasNext()) {
								String key = keys.next();
								joCurrent.put(key, joRef.get(key));
							}
						}
						
					} else {
						throw new JapathException("ref '' not resolvable");
					}
				}
			}
			if (level == 2) {
				JSONObject copy = new JSONObject(joCurrent.toString());
				List<String> keys = Basics.streamIt(joCurrent.keys()).collect(Collectors.toList());
				keys.forEach(
						key -> joCurrent.remove(key)
						);
				joCurrent.put("value", copy);
			}
		} else if (current instanceof JSONArray ja) {
			
			ja.forEach(x -> {
				resolvePrototypeBundle_trav(x, root, level);
			});
		}
	}

	public Node resolvePrototypeBundle() {
		if (prototypeBundle == null) throw new JapathException("not prototype bundle set");
		return resolvePrototypeBundle(prototypeBundle);
	}

	public Node resolvePrototypeBundle(Node prototypeBundle) {
		
		// we need json.org for json pointers
		JSONObject root = new JSONObject(prototypeBundle.woString(0));
		
		resolvePrototypeBundle_trav(root, root, 0);
		return NodeFactory.w_(root.toString());
	}
	

	public JsonSchemaProcessing setOpt(boolean opt) { this.opt = opt; return this; }

	public JsonSchemaProcessing setModular(boolean modular) { this.modular = modular; return this; }

	public JsonSchemaProcessing setSchemaBundle(Node schemaBundle) {
		this.schemaBundle = schemaBundle;
		return this;
	}

	public JsonSchemaProcessing setOnlyTopModular(boolean onlyTopModular) {
		this.onlyTopModular = onlyTopModular;
		return this;
	}

	public Node getSchemaBundle() { return schemaBundle; }

	public JsonSchemaProcessing setComplete(boolean complete) {
		this.complete = complete;
		return this;
	}

	public Node getPrototypeBundle() { return prototypeBundle; }

}