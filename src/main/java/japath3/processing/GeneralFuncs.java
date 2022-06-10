package japath3.processing;

import java.util.HashSet;
import java.util.Set;

import japath3.core.Japath;
import japath3.core.Japath.NodeIter;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.util.Basics;

public class GeneralFuncs { // available by ns 'it' or 'gen'

	public NodeIter distinct(Node ctxNode, NodeIter[] nits) {
		
		if (nits.length == 0 || nits.length > 1) throw new JapathException("'set' must have exactly one argument");
		
		Set mem = new HashSet();
		
		return Japath.nodeIter(Basics.stream(nits[0].iterator()).filter(x -> {
			return !mem.contains(x.val().toString());
		}).map(x -> {
			mem.add(x.val().toString());
			return x;
		}).iterator());
		
	}
	
	public NodeIter project(Node ctxNode, NodeIter[] nits) {
		
		if (nits.length == 0 || nits.length > 1) throw new JapathException("'set' must have exactly one argument");
		
		Node p = ctxNode.create(ctxNode.createWo(false), "");
		
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
}
