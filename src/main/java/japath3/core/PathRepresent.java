package japath3.core;

import static japath3.wrapper.NodeFactory.w_;

import java.util.ArrayList;
import java.util.List;

import com.florianingerl.util.regex.Pattern;

import japath3.core.Japath.Assignment;
import japath3.core.Japath.Expr;
import japath3.util.Coding;
import japath3.wrapper.NodeFactory;

public class PathRepresent {
	
	@FunctionalInterface
	public static interface LeafObjectProducer {
		public Object apply(Node leaf);
	}

	// for path regex'es
	private static Pattern regexTrans1 = Pattern.compile("'(.)");
	private static Pattern regexTrans2 = Pattern.compile("\\.");
	private static Pattern regexTrans3 = Pattern.compile("~");


	private Coding fieldCoding = new Coding('_').setAllowedCharsRegex(Coding.IdRegex);
	private Coding singleIntCoding = new Coding('_').setAllowedCharsRegex("");
	
	private String prefix;
	private String preserveTopPropRegex;
	private boolean leafArray;
	
	public PathRepresent() {}
	
	public PathRepresent(String prefix) {
		this.prefix = prefix;
	}
	
	public Node toFlatNode(Node n) {
		return toFlatNode(n, null);
	}
	
	public Node toFlatNode(Node n, String skipProp) {
		return toFlatNode(n, skipProp, leaf -> {
			return leaf.woVal();
		});
	}
	
	public Node toFlatNode(Node n, String skipProp, LeafObjectProducer lop) {

		Node ret = NodeFactory.w_();

		for (Node leaf : n.leafNodes(leafArray)) {
			
			if (leaf.previousNode == n) {
				String sel = leaf.selector.toString();
				
				if (skipProp != null && sel.equals(skipProp)) continue;
				
				if (preserveTopPropRegex != null && sel.matches(preserveTopPropRegex)) {
					ret.set(sel, leaf.woVal());
					continue;
				}
			}
			ret.set(selectorPath(leaf), lop.apply(leaf));
		}
		return ret;
	}

	public String selectorPath(Node n) {
		String p = "";
		for (Node x : n.nodePathToRoot()) {
			if (x.previousNode != null) p = extendPath(p, x);
		}
		// remove '.'-tail
		p = (prefix != null ? prefix + "." : "")
				+ p.replaceAll("\\.$", "");
		//
		return p;
	}
	
	public Node toPathSchema(Node n) {
		return toFlatNode(n, null, x -> {
//			String t = x.isLeaf() ? 
			return w_().set("type", x.wo.getClass().getName()).woVal();
		});
	}

	private String extendPath(String p, Node x) {
		
		Object selector = x.selector;
		boolean singleInt = selector instanceof String s && s.matches("\\d+");
		if (selector instanceof String s && s.indexOf("'") != -1) throw new JapathException("' in names names not allowed");
		
		// single int's only have to be encoded
		p += ( singleInt ? singleIntCoding : fieldCoding).encode(selector.toString()) + (x.isLeaf() ? "" : ".");
		return p;
	}

//	private 
	
	public Node toStructNode(Node flatnode) {
		
		Node n = NodeFactory.w_().setConstruct(true);

		for (Node x : flatnode.all()) {
			
			String sel = x.selector.toString();
//			if (preserveTopPropRegex != null && x.previousNode.previousNode == null && sel.matches(preserveTopPropRegex)) {
			if (preserveTopPropRegex != null && x.previousNode == flatnode && sel.matches(preserveTopPropRegex)) {
				n.set(sel, x.woVal());
			} else {
				if ((prefix != null && !sel.startsWith(prefix + ".")))
					continue;
				buildAssignment(sel, (Object) x.val()).eval(n);				
			}
		}

		return n;
	}

	private Assignment buildAssignment(String path, Object val) {
		
		List<Expr> exprs = new ArrayList<>();
		for (String s : path.split("\\.")) {
			if (s.equals(prefix)) continue;
			if (s.matches("\\d+")) {
				exprs.add(Japath.__(Integer.valueOf(s)));
			} else {
				exprs.add(Japath.__(true, fieldCoding.decode(s)));
			}
		}
		return Japath.assign(Japath.path(exprs.toArray(new Expr[exprs.size()])), val);
	}
	
	public String encodePathRegex(String r) {
		
		r = regexTrans1.matcher(r).replaceAll(m -> {
			return "(" + fieldCoding.encode(m.group(1)) + ")";
		});

		r = regexTrans2.matcher(r).replaceAll(m -> {
			return "\\.";
		});

		r = regexTrans3.matcher(r).replaceAll(m -> {
			return ".";
		});
		
		return r;
	}
	
	public Iterable<Node> process(Node flatnode, String regex) {
		
		flatnode.setConstruct(true);
		final String regex_ = encodePathRegex(regex);
		
		return io.vavr.collection.List.ofAll(flatnode.all()).filter(x -> {
			return x.selector.toString().matches(regex_);
		});
	}
	
	public boolean containsSubscripts(String path) {
		String d = "\\d+";
		return path.matches(".*\\." + d + "(\\..*|$)|" + "^" + d + "\\..*");
	}

	public PathRepresent setLeafArray(boolean leafArray) {
		this.leafArray = leafArray;
		return this;
	}

	public PathRepresent setFieldCoding(Coding fieldCoding) {
		this.fieldCoding = fieldCoding;
		return this;
	}

	public PathRepresent setPreserveTopPropRegex(String preserveTopPropRegex) {
		if (prefix != null && prefix.matches(preserveTopPropRegex))
			throw new JapathException("prefix '' collides with 'preserveTopPropRegex' ('" + preserveTopPropRegex + "')");
		this.preserveTopPropRegex = preserveTopPropRegex;
		return this;
	}
}
