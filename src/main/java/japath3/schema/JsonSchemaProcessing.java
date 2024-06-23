package japath3.schema;

import static japath3.util.Basics.it;
import static japath3.wrapper.NodeFactory.emptyObject;
//import static japath3.wrapper.NodeFactory.w_;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONPointer;

import com.florianingerl.util.regex.Pattern;

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
import japath3.util.Regex;
import japath3.wrapper.NodeFactory;
import japath3.wrapper.WGson;

/**
 * 
 */
public class JsonSchemaProcessing {
	
	static class Defs {
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
			return createObjNode().set("$ref", "#/$defs/" + name); 
		}
	}
	
	static record PropertyAnnotation(boolean opt_, MinMax minmax) {
		
		static record MinMax(String min, String max, boolean allowEmpty) {}
	
		static String a = "( \\| (?<opt>\\+|\\-) )? (?<card> \\| (?<emp>\\?)? (?<min>\\d+)..(?<max>\\d+|\\*) )? ".replace(" ", "\\s*");
		static String m = "[^\\|]+" + a;
		static String a_ = a + "\"";

		public static PropertyAnnotation eval(String prop, boolean optDefault) {
			
			if (prop.contains("|")) {
				
				String[] anno = Regex.multiExtract(prop, m, null, null, null, null, null, "0", null);
				var map = Regex.multiExtract(prop, m, new HashMap<>() {
					{
						put("opt", null);
						put("emp", null);
						put("card", null);
						put("min", "0");
						put("max", null);
					}
				});
				if (anno == null) throw new JapathException(Regex.getErrorString(Pattern.compile(m), prop));

				return new PropertyAnnotation( //
						map.get("opt") != null ? map.get("opt").equals("-") : optDefault,
						map.get("card") == null ? null : new MinMax(map.get("min"), map.get("max"), map.get("emp") != null));
			} else {
				return null;
			}
			
		}
		
		public static String removeAnnotation(String text, boolean inclQuote) { 
			return inclQuote ? text.replaceAll(a_, "\"") : text.replaceAll(a, ""); 
		}
	}
	
	private boolean optDefault;
	private boolean complete = true;
	private boolean modular = true;
	private boolean onlyTopModular = true;
	private Defs defs = new Defs();
	
	private Node schemaBundle;
	private Node prototypeBundle;
	private Node resolvedPrototypeBundle;
	private static GsonNode.Factory gsonNodeFactory = new GsonNode.Factory();
	private static ValidatorFactory validator = new ValidatorFactory().withJsonNodeFactory(gsonNodeFactory);
	
	
	private ProtoInjections protoInjections;
	
//	public JsonSchemaProcessing() {
//		
//	}
	
	public Node buildJsonTopSchema(Node prototypeBundle) {
		
		protoInjections = new ProtoInjections(prototypeBundle);
		Node root = prototypeBundle.node("$defs").detach();

		Node js = buildJsonSchema(root, root, 0);
		Node ret = createObjNode().set("$schema", "https://json-schema.org/draft/2020-12/schema")
//				.set("$id", UUID.randomUUID().toString())
				;
		
		String topRef = js.val("$ref", null);
		if (modular && topRef != null) {
			ret.set("$ref", topRef);
		} else {
			for (Node jsn : js.all()) ret.set(jsn.selector.toString(), jsn.val()) ; 
		}
		
		if (!defs.nameToNode.isEmpty()) {
			
			Node defs_= createObjNode();
			for (Tuple2<String, Node> def : defs.nameToNode) {
				defs_.setNode(def._1, def._2);
			}
			ret.setNode("$defs", defs_);
		}
		return ret;
	}

	private static Node createObjNode(String s) { 
		return NodeFactory.w_(s, WGson.class); 
	}
	private static Node createObjNode() { 
		return NodeFactory.w_(emptyObject, WGson.class); 
	}
	private Node createArrayNode() { 
		return NodeFactory.w_(NodeFactory.emptyArray, WGson.class); 
	}
	
	private Node buildJsonSchema(Node n, Node root, int level) {

		Node injections = protoInjections.getInjections(n);

		if (n.isStruct()) {

			Node propTypes;
			Node reqProps = null;
			Node structType = createObjNode().set("type", "object")
					.setNode("properties", propTypes = createObjNode());
			if (!optDefault) structType.setNode("required", reqProps = createArrayNode());
			if (complete && !n.isEmpty()) structType.set("additionalProperties", false);
			
			String use = n.val("$ref", null);
			if (use != null) {
				if (!modular) throw new JapathException("$ref only allowed in modular mode");
				if (n.length() > 1) throw new JapathException("$ref must be the only property");
				return n;
			}
			
			/// handle proto spec
			
			boolean allOptional = injections.val("$proto:allOptional", false);
			boolean ignore = injections.val("$proto:ignore", false);
			String optionalPropRegex = injections.val("$proto:optional", null);
//			Set<String> optionalProps = 
//					opt_ != Node.nil ? 
//							Basics.streamIt(opt_.all()).map(x -> {
//								return (String) x.val();
//								}).collect(Collectors.toSet()) 
//							: Collections.emptySet();
			injectToTypeNode(injections, structType, true);
			///
			
			for (Node x : it(n.all())) {
				String sel = x.selector.toString();
				if (sel.matches("^\\$proto\\:.*")) continue;
				// handle annotations
				PropertyAnnotation pa = PropertyAnnotation.eval(sel, optDefault);
				if (pa != null) {
					sel = PropertyAnnotation.removeAnnotation(sel, false);
					if (!pa.opt_) {
						if (reqProps == null) structType.setNode("required", reqProps = createArrayNode());
						reqProps.add(sel);
					}
				} else {
					if (!optDefault && !allOptional && (optionalPropRegex != null ? !sel.matches(optionalPropRegex) : true)) 
						reqProps.add(sel);
				}
				//
				propTypes.setNode(sel, buildJsonSchema(x, root, level + 1));
			}

			return ignore ? Node.nil : buildStructNode(structType, new PathRepresent().selectorPath(n), level);

		} else if (n.isArray()) {

			Node itemTypes = createArrayNode();
			Set<String> mem = new HashSet<>();

			int i = 0;
			for (Node x : it(n.all())) {
				Node js = buildJsonSchema(x, root, level + 1);
				if (js != Node.nil && mem.add(removedExamplesClone(js).toString())) {
					itemTypes.addNode(js);
					i++;
				}
			}
			Node arrayTypes = createObjNode().set("type", "array")
					.setNode("example", n)
					;
			
			if (i != 0) {
				arrayTypes.setNode("items", i > 1 ? createObjNode().setNode("anyOf", itemTypes) : itemTypes.node(0));
			}			
			
			// handle property annotations
			PropertyAnnotation pa = PropertyAnnotation.eval(n.selector.toString(), optDefault);
			Node anyOf = null;
			if (pa != null && pa.minmax != null) {
				arrayTypes.set("minItems", Integer.valueOf(pa.minmax.min));
				if (!pa.minmax.max.equals("*")) arrayTypes.set("maxItems", Integer.valueOf(pa.minmax.max));
				if (pa.minmax.allowEmpty) {
					anyOf =
							createObjNode().setNode("anyOf",
									createArrayNode().addNode(
											createObjNode().set("type", "array").set("maxItems", 0)
//												.setNode("items", createArrayNode()) // does'nt work for harrel
											)
											.addNode(arrayTypes));
				}
			}
			//

			injectToTypeNode(injections, arrayTypes, false);
			return anyOf == null ? arrayTypes : anyOf;
		} else {
			Node typeNode = createObjNode().set("type", deriveType(n)).set("example", n.val());
			injectToTypeNode(injections, typeNode, false);
			return typeNode;
		}

	}

//	private void injectToTypeNode(Node injections, Node typeNode) {
//		injectToTypeNode(injections, typeNode, false);
//	}
	
	private void injectToTypeNode(Node injections, Node typeNode, boolean isObject) {
		for (Node spec : injections.all()) {
			String sel = spec.selector.toString();
			if (!sel.startsWith("$proto:") && !(sel.equals("additionalProperties") && !isObject)) {
				typeNode.set(sel, spec.val());
			}
		}
	}
	
	private Node removedExamplesClone(Node js) {
		Node copy = NodeFactory.w_(js.woString(0));
		copy.removeAll("example");
		return copy;
	}

	private Node buildStructNode(Node structType, String sel, int level) {
		boolean m = modular && (onlyTopModular ? level <= 1 : true);
		return defs.register(m, structType, sel); 
	}

	private String deriveType(Node n) {

		if (n.isNull()) return "null";
		
		Object val = n.val();
		String t = val instanceof String ? "string" //
//				: val instanceof Integer ? "integer" //
				: val instanceof Number ? "number" //
						: val instanceof Boolean ? "boolean" : null;
		if (t == null) throw new JapathException("primitive type '" + val.getClass() + "' not convertable");
		return t;
	}
	
	public Option<List<Error>> validate(Node instance, String name) {
		
		
		Node defs_ = schemaBundle.node("$defs");
		if (defs_.node(name) == Node.nil) throw new JapathException("subSchema '" + name + "' not found");

		Node alignedToSubSchema =
				createObjNode().set("$ref", "#/$defs/" + name).set("$defs", defs_.val());
		
		Result result =
				validator.validate(gsonNodeFactory.wrap(alignedToSubSchema.woVal()), gsonNodeFactory.wrap(instance.woVal()));
		
		return result.isValid() ? Option.none() :  Option.of(result.getErrors());
	}
	
	public static String errorText(List<Error> errors) {
		
		return errors.stream().map(
				error -> {return error.getInstanceLocation() + ": " + error.getError();}
				).collect(Collectors.joining("\n"));
	}
	
	public JsonSchemaProcessing usePrototypeBundle(Node prototypeBundle) {
		
		this.prototypeBundle = createObjNode(PropertyAnnotation.removeAnnotation(prototypeBundle.woString(0), true));
		setSchemaBundle(buildJsonTopSchema(prototypeBundle));
		resolvePrototypeBundle();
		return this;
	}
	
	
	private Node resolvePrototypeBundle_trav(Stack hits, Object current, JSONObject root, int level) {
		
		if (current instanceof JSONObject joCurrent) {
			
			Node n = createObjNode();
			
			String ref = joCurrent.optString("$ref", null);
			if (ref != null) {
				if (ref.matches("^(\\/|\\#\\/).*")) {

					if (new JSONPointer(ref).queryFrom(root) instanceof JSONObject joRef) {
						if (hits.contains(joRef)) {
							throw new JapathException("ref '" + ref + "' yields a cycle");
						} else {
							hits.add(joRef);
						}
						Node ret = resolvePrototypeBundle_trav(hits, joRef, root, level + 1);
						hits.pop();
						return ret;
					} else {
						throw new JapathException("ref '" + ref + "' not resolvable");
					}
				} else {
					throw new JapathException("ref '" + ref + "' must not be external");
				}
			}
			
			List<String> props = Basics.streamIt(joCurrent.keys()).collect(Collectors.toList());
			for (String prop : props) {
				if (!prop.startsWith("$proto:"))
					n.setNode(prop, resolvePrototypeBundle_trav(hits, joCurrent.get(prop), root, level + 1));
			}
//			return n;
			return level == 2 ? createObjNode().setNode("value", n) : n ;
		} else if (current instanceof JSONArray ja) {
			
			Node n = createArrayNode();
		
			ja.forEach(x -> {				
				n.addNode(resolvePrototypeBundle_trav(hits, x, root, level + 1));
			});
			return n;

		} else { // primitive
			Node h = createObjNode();
			return new Node.DefaultNode(h.createWo(current), h.ctx);
		}
	}


	private void resolvePrototypeBundle() {
		if (prototypeBundle == null) throw new JapathException("not prototype bundle set");
		// we need json.org for json pointers
		JSONObject root = new JSONObject(prototypeBundle.woString(0));
		Stack hits = new Stack();
		resolvedPrototypeBundle = resolvePrototypeBundle_trav(hits, root, root, 0);		
	}
	
	public Node getExample(String type) {
		
		Node exa = resolvedPrototypeBundle.node("$defs").node(type). node("value");
		if (exa == Node.nil) throw new JapathException("example for type '" + type + "' not found");
		return exa; 
	}


	public JsonSchemaProcessing setOptDefault(boolean opt) { this.optDefault = opt; return this; }

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

	public Node getResolvedPrototypeBundle() { return resolvedPrototypeBundle; }


}
