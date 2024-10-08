package japath3.processing;

import java.util.LinkedHashSet;
import java.util.Set;

import japath3.core.Japath;
import japath3.core.Japath.NodeIter;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.core.PathRepresent;
import japath3.util.Basics;
import japath3.util.Coding;

public class GeneralFuncs { // available by ns 'it' or 'gen'
	
	private PathRepresent pathRepresent;

	public GeneralFuncs() {
		pathRepresent = new PathRepresent().setFieldCoding(new Coding('_').setSpecialChars("."));
	}
	
	public NodeIter distinct(Node ctxNode, NodeIter[] nits) {
		
		if (nits.length == 0 || nits.length > 1) throw new JapathException("'set' must have exactly one argument");
		
		Set mem = new LinkedHashSet();
		
		return Japath.nodeIter(Basics.stream(nits[0].iterator()).filter(x -> {
			return !mem.contains(x.val().toString());
		}).map(x -> {
			mem.add(x.val().toString());
			return x;
		}).iterator());
		
	}
	
	public NodeIter sort(Node ctxNode, NodeIter[] nits) {
		
		if (nits.length == 0 || nits.length > 1) throw new JapathException("'sort' must have exactly two arguments (node iterator and key selection)");
		
//		if (!ctxNode.isArray()) throw new JapathException("context node must be an array"); 
		
		return null;
		
		
	}
	
	public NodeIter project(Node ctxNode, NodeIter[] nits) {
		
		if (nits.length == 0 || nits.length > 1) throw new JapathException("'project' must have exactly one argument");
		
		Node p = ctxNode.create(ctxNode.createWo(false), "");

		// alternative !?
//		for (NodeIter nit : nits) {
//			Object name = nit.val();
//			if (nit.hasNext()) throw new JapathException("argument must be a single value");

		Basics.stream(nits[0].iterator()).forEach(x -> {
			Object name = x.val();
			if (name instanceof String) {
				Object val = ctxNode.get(name.toString()).val(null);
				if (val != null) p.set(name.toString(), val);
			} else {
				throw new JapathException("error during projection: '" + name + "' is not a string");
			}
		});		

		return Japath.single(p);
	}
	
	public NodeIter parent(Node ctxNode) {
		
		Node previousNode = ctxNode.previousNode;
		
		if (previousNode == null)
			throw new JapathException("no parent of '" + ctxNode + "'\n because it is a root node");
		
		return Japath.single(previousNode);
		
	}
	
	public boolean isLeaf(Node ctxNode) {
		
		return ctxNode.isLeaf();
	}
	
	public boolean isArray(Node ctxNode) {
		
		return ctxNode.isArray();
	}
	
	public String selectorPath(Node ctxNode) {
		return pathRepresent.selectorPath(ctxNode);
	}
	
	public NodeIter clone(Node ctxNode) throws CloneNotSupportedException {
		
		return Japath.single((Node) ctxNode.clone());
	}
}
