package japath3.schema;


import io.vavr.collection.List;
import io.vavr.collection.Map;
import japath3.core.Japath;
import japath3.core.Japath.NodeIter;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.core.PathRepresent;
import japath3.processing.Language;
import japath3.util.Basics;
import japath3.wrapper.NodeFactory;
import japath3.wrapper.WGson;

class ProtoInjections {
		
		// instance node-path-to-root -> list of annotation nodes
		private Map<String, List<Node>> map;
		
		public ProtoInjections(Node prototypeBundle) {

			map = io.vavr.collection.HashMap.empty();
			
			String prefix = "`$defs`.";
			Node pa = prototypeBundle.node("$proto:injections:json-schema");    
			if (pa != Node.nil) {
				for (Node inj : pa.all()) {
					Object targets = inj.val("$proto:targets", null);
					if (targets == null) throw new JapathException("prototype json error: property '$proto:targets' not defined");
					try {
						// TODO needed cause bundle is Gson
						Class<?> c = NodeFactory.setDefaultWrapperClass(WGson.class);
						Iterable<Node> walki = Japath.walki(prototypeBundle, Language.e_(prefix + targets));
						//
						for (Node n : walki) {
							String selectorPath = new PathRepresent().selectorPath(n);
							map = Basics.putExtend(map, selectorPath, inj);
						}
						NodeFactory.setDefaultWrapperClass(c);
					} catch (JapathException e) {
						throw new JapathException("prototype json error: bad path at property \"$proto:targets\": \"" + prefix + targets
								+ "\"\n"
								+ e.getMessage());
					}
				}
			}
		}

		public Node getInjections(Node n) {
			
			String codedPrefix = "_24_defs.";
			
			String selectorPath = codedPrefix + new PathRepresent().selectorPath(n);
			Node resolvedAnotation = NodeFactory.w_();
			
			List<Node> al = map.getOrElse(selectorPath, null);
			if (al != null) {
				
				for (Node a : al) {
					NodeIter all = a.all();
					for (Node p : all) {
						String sel = p.selector.toString();
						if (!sel.startsWith("$proto:")) resolvedAnotation.setNode(sel, p);
					}
				}
			}
			
			return resolvedAnotation; 
		}
	}