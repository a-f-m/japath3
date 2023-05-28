package japath3.core;

import java.util.ArrayList;
import java.util.List;

import com.florianingerl.util.regex.Pattern;

import japath3.core.Japath.Assignment;
import japath3.core.Japath.Expr;
import japath3.util.Coding;
import japath3.wrapper.NodeFactory;

public class PathRepresent {

	// for path regex'es
	private static Pattern regexTrans1 = Pattern.compile("'(.)");
	private static Pattern regexTrans2 = Pattern.compile("\\.");
	private static Pattern regexTrans3 = Pattern.compile("~");


	private Coding fieldCoding = new Coding('_').setAllowedCharsRegex(Coding.IdRegex);
	
	private String prefix;
	private boolean leafArray;
	
	public PathRepresent() {}
	
	public PathRepresent(String prefix) {
		this.prefix = prefix;
	}

	public Node toFlatNode(Node n) {

		Node ret = NodeFactory.w_();

		for (Node leaf : n.leafNodes(leafArray)) {
			String p = prefix != null ? prefix + "." : "";
			for (Node x : leaf.nodePathToRoot()) {
				if (x.previousNode != null) p = extendPath(p, x);
			}
			// remove '.'-tail
			p = p.replaceAll("\\.$", "");
			//
			ret.set(p, leaf.woVal());
		}
		return ret;
	}

	private String extendPath(String p, Node x) {
		
		// check int form
		if (x.selector instanceof String s && s.matches("\\d+")) throw new JapathException("int names not allowed");
		if (x.selector instanceof String s && s.indexOf("'") != -1) throw new JapathException("' in names names not allowed");
		//
		String selector = x.selector.toString();
		p += fieldCoding.encode(selector) + (x.isLeaf() ? "" : ".");
		return p;
	}
	
	public Node toStructNode(Node flatnode) {
		
		Node n = NodeFactory.w_().setConstruct(true);

		for (Node x : flatnode.all()) buildAssignment(x.selector.toString(), (Object) x.val()).eval(n);

		return n;
	}

	private Assignment buildAssignment(String path, Object val) {
		
		List<Expr> exprs = new ArrayList<>();
		for (String s : path.split("\\.")) {
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
		System.out.println(r);

		r = regexTrans2.matcher(r).replaceAll(m -> {
			return "\\.";
		});
		System.out.println(r);

		r = regexTrans3.matcher(r).replaceAll(m -> {
			return ".";
		});
		System.out.println(r);
		
		return r;
	}

	public PathRepresent setLeafArray(boolean leafArray) {
		this.leafArray = leafArray;
		return this;
	}

	public PathRepresent setFieldCoding(Coding fieldCoding) {
		this.fieldCoding = fieldCoding;
		return this;
	}
}
