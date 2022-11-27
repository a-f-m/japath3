package japath3.core;

import static japath3.core.Japath.empty;
import static japath3.core.Japath.nodeIter;
import static japath3.core.Japath.single;
import static japath3.core.Japath.walkr;
import static japath3.core.Japath.NodeProcessing.Kind.Post;
import static japath3.core.Japath.NodeProcessing.Kind.Pre;
import static japath3.processing.Language.stringify;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import japath3.core.Japath.Assignment;
import japath3.core.Japath.Assignment.Scope;
import japath3.core.Japath.NodeIter;
import japath3.core.Japath.NodeProcessing.Kind;
import japath3.core.Japath.Selection;
import japath3.schema.Schema;
import japath3.util.Basics.Ref;

/**
 *
 * For efficiency reasons primitive java arrays and collections are used in parallel to the functional vavr lib. 
 * In order to be compact, 'single line'-code is preferred (similar to functional languages like scala).
 * 
 * @author andreas-fm
 *
 */
public abstract class Node implements Cloneable {

	public static class DefaultNode extends Node {

		public DefaultNode(Object wo, Ctx ctx) { super(wo, "", null, ctx); }

		@Override public NodeIter get(String name) {
			throw new UnsupportedOperationException("get('" + name + "') not supported for '" + this + "'");
		}
		@Override public NodeIter get(int i) {
			throw new UnsupportedOperationException("get(" + i + ") not supported for '" + this + "'");
		}
		@Override public NodeIter all(Object o) {
			throw new UnsupportedOperationException("'all' not supported for '" + this + "'");
		}
		@Override public NodeIter desc() {
			throw new UnsupportedOperationException("'desc' not supported for '" + this + "'");
		};
		@Override public PrimitiveType type() {
			throw new UnsupportedOperationException("'type' not supported for '" + this + "'");
		};
		@Override public boolean isLeaf() { return true; };
		@Override public boolean isArray() { return false; }
		@Override public boolean isNull() { return false; }
		@Override public NodeIter text() {
			return Japath.singleObject(toString(), this);
			// throw new UnsupportedOperationException("'text()' not supported at
			// '" + this + "'");
		};
		
		@Override public Object woCopy() {
			return wo;
		}
	}

	public static enum PrimitiveType {
		String, Number, Boolean, Any
	}

	protected Object wo; // wrapped object
	public boolean construct;
	public Object selector;
	public int order;
	public Ctx ctx;
	public Node previousNode;
	
	public static Object nilo = new Object() {
		@Override public String toString() { return "nilo"; }
	};
	public static Node nil = new DefaultNode(nilo, new Ctx()) {
		public Object val() { return nilo; };
		@Override public String toString() { return "nil"; }
	};
	
	public static class Null implements Comparable {
		@Override public int compareTo(Object o) {
			if (!(o instanceof Null)) throw new JapathException("can only be compared to 'Null'");
			return 0; 
		}
		@Override public String toString() { return "null"; }
	}

	public static Object nullo = new Null();
	
	public static Object undefWo = new Object() {
		@Override public String toString() { return "undef"; }
	};

	public Node(Object wo, Object selector, Node previousNode, Ctx ctx) {
		this.wo = wo;
		this.selector = selector;
		this.ctx = ctx;
		this.previousNode = previousNode;
		if (previousNode != null) {
			construct = previousNode.construct;
		}
	}
	
	// TODO deferred
//	public Node copy() { return create(woCopy(), selector, previousNode, ctx).setConstruct(construct).setOrder(order); }
	
	public Node create(Object x, Object selector) {
		return create(x, selector, this, ctx);
	}
	public Node create(Object wo, Object selector, Node previousNode, Ctx ctx) {
		throw new UnsupportedOperationException("'create()' not supported for '" + this + "'");
	};
	public Object createWo(boolean array) { throw new UnsupportedOperationException("'createWo()' not supported for '" + this + "'"); };
	public NodeIter undef(Object sel) { return single(create(undefWo, sel, this, ctx)); }

	private NodeIter determRet(Object selector, NodeIter nit, Selection sel) {
		
		NodeIter ret;
		if (construct) {
			if (wo == undefWo) {
				if (sel.scope != Scope.lhs) 
					throw new JapathException("only construction at lhs allowed" + selectionLog(sel));
				ret = undef(selector);
			} else if (nit != empty) {
				ret = nit;
			} else {
				if (sel.scope == Scope.rhs) 
					throw new JapathException("rhs has undef value" + selectionLog(sel));
				ret = sel.scope != Scope.lhs ? nit : undef(selector);
			}
		} else {
			if (nit != empty) {
				ret = nit;
			} else {
//				if (sel.scope == Scope.rhs) 
//					throw new JapathException("rhs has undef value" + ssel);
				ret = nit;
			}
		}
		return ret;
	}

	private String selectionLog(Selection sel) { return " (selection: '" + stringify(sel, 0) + "'; node: '" + this + "')"; }
	
	public abstract NodeIter get(String name);
	public abstract NodeIter get(int i);
	
	public NodeIter getChecked(String name, Selection sel) {
		Schema schema = ctx.getSchema();
		if (schema != null && schema.genCompleteness()) {
			schema.addPropHit(this, name);
		}
		return determRet(name, get(name), sel);
	}
	public NodeIter getChecked(int i, Selection sel) {
		return determRet(i, get(i), sel);
	}

	public boolean isAttribute(String name) { return false; };
	public Node set(String name, Object o) { throw new UnsupportedOperationException("'set(name)' not supported for '" + this + "'"); };
	public Node set(int idx, Object o) { throw new UnsupportedOperationException("'set(i)' not supported for '" + this + "'"); };
	public Node add(Object o) { return set(-1, o); };
	public void remove(Object selector) { throw new UnsupportedOperationException("'remove(name)' not supported for '" + this + "'"); };
	public Iterator<String> childrenSelectors() { throw new UnsupportedOperationException("'childrenSelectors' not supported for '" + this + "'"); }
	public NodeIter all() { return all(wo); };
	public abstract NodeIter all(Object o);
//	public abstract NodeIter desc();
	public NodeIter desc() {
//		old way:
//		ArrayList<Node> descs = new ArrayList<Node>();
//		gatherDesc(descs, create(wo, selector, previousNode, ctx).setConstruct(construct));
//		return nodeIter(descs.iterator());
		return descWalk(false);
	}
	public NodeIter descWalk(boolean bottomUp) {
		ArrayList<Node> descs = new ArrayList<Node>();
		Japath.walkr(this, (x, kind, level, orderNo, isLast) -> {
			if ( !bottomUp && kind == Pre)
				descs.add(x);
			if ( bottomUp && kind == Post)
				descs.add(x);
		});
		return nodeIter(descs.iterator());
	}
//	public abstract NodeIter text();
	public NodeIter text() { return Japath.singleObject(wo.toString(), previousNode); }
	public boolean isCheckedLeaf() {
		if (wo == undefWo) throw new JapathException("operation on undef node '" + this + "' (assign value first)");
		else return isLeaf();
	}
	public abstract boolean isLeaf();
	public boolean isStruct() { return !isCheckedArray() && !isCheckedLeaf(); };
	public boolean isCheckedArray() {
		if (wo == undefWo) throw new JapathException("operation on undef node '" + this + "' (assign value first)");
		else return isArray();
	}
	public abstract boolean isArray();
	public boolean isNull() { throw new UnsupportedOperationException("'isNull()' not supported for '" + this + "'"); }
	public Object nullWo() { throw new UnsupportedOperationException("'nullWo()' not supported for '" + this + "'"); }
	public boolean hasIdxSelector() { return selector instanceof Integer; }	
	public boolean type(PrimitiveType t) { throw new UnsupportedOperationException("'type(t)' not supported for '" + this + "'"); };
	public PrimitiveType type() { throw new UnsupportedOperationException("'type()' not supported for '" + this + "'"); };
	public abstract Object woCopy();

	public void gatherDesc(List<Node> descs, Node node) {
		// TODO here we materialize all descendants. future work should iterate.
		descs.add(node);
		all(node.val()).forEachRemaining(x -> {
			gatherDesc(descs, x.setPreviousNode(node));
		});
	}
	
	public boolean exists(Object selector) {throw new UnsupportedOperationException("'exists(sel)' not supported for '" + this + "'"); }; // TODO use it in set
	public Node seto(Object selector, Object o) { // TODO undef/overwritable?
		if (!construct) throw new JapathException("input node not modifyable: " + this);
		// TODO not for parents
//		else if (exists(selector)) throw new JapathException("'" + selector + "' already set: " + this);
		if (o == nullo) o = nullWo();
		return selector instanceof Integer i ? set(i, o) : set((String) selector, o);
	}
	public Node node(String name) { return get(name).node(); };
	public Node node(int i) { return get(i).node(); };
	public <T> T val(String name) { return get(name).val(); };
	public <T> T val(String name, T d) { return get(name).val(d); };
	public <T> T val(int i) { return get(i).val(); };
	public <T> T val(int i, T d) { return get(i).val(d); };
	public <T> T val() {
		if (wo == undefWo) throw new JapathException("operation on undef node '" + this + "' (assign value first)");
		return (T) woVal(); 
	}
	public Object woVal() { return wo; }
	public Node setCtx(Ctx ctx) {
		this.ctx = ctx;
		return this;
	}
	public void rename(String oldName, String newName) {
		Object h = val(oldName, null);
		if (h != null) {
			remove(oldName);
			set(newName, h);
		}
	}
	public Node setPreviousNode(Node n) {
		this.previousNode = n;
		return this;
	}
	public Node setOrder(int order) {
		this.order = order;
		return this;
	}

	public Node setConstruct(boolean construct) {
		this.construct = construct;
		return this;
	}

	public void toRoot(Consumer<Node> c) {
		Node x = this;
		while (x != null) {
			c.accept(x);
			x = x.previousNode;
		}
	}
	public List<Node> nodePath() {
		List<Node> l = new ArrayList<Node>();
		toRoot(x -> {
			l.add(0, x);
		});
		return l;
	}
	public List<Object> selectorPath() {
		return nodePath().stream().map(x -> {
			return x.selector;
		}).collect(Collectors.toList());
	}
	
	public void setAncestors(Assignment assignment) {
		
		for (Node x: io.vavr.collection.List.ofAll(this.nodePath()).reverse()) {
			// recursion failure
			if (x != this && x.wo == this.wo) {
				throw new JapathException("resursion caused by " + (assignment == null ? "internal" : assignment));
			}
			Node prev;
			if ((prev = x.previousNode) != null) {
				if (prev.wo == undefWo) {
					prev.wo = prev.createWo(x.hasIdxSelector());
				}
				prev.seto(x.selector, x.wo);
//				prev.setConstruct(true);
			}
//			x.setConstruct(true);
		}
	}
	
	public Set<String> selectorNameSet() {
		
		Ref<Set<String>> ret = Ref.of(TreeSet.empty());
		walkr(this, (x, kind, level, orderNo, isLast) -> {
			if (kind == Kind.Pre) {
				if (x.selector instanceof String) ret.r = ret.r.add(x.selector.toString());
			}
		});
		return ret.r;
		
	}
	
	public Node freshNode() {
		return freshNode(false);
	}
	public Node freshNode(boolean array) {
		return create(createWo(array), "");
	}

	public Node prefixPropertyNames(String prefix) {
		
		Node n = freshNode(false);
		
		for (Node n_ : all()) n.set(prefix + n_.selector, get(n_.selector.toString()).val());
		
		return n;
		
	}


	public <T> T v(String name) {
		
		return ctx.getVars().val(name);
	}
	
	@Override public Object clone() throws CloneNotSupportedException { 

		Node clone = (Node) super.clone();
		clone.wo = woCopy();
		return clone; 
	}
	
	public String woString(int indent)  { throw new UnsupportedOperationException("'toString(int indent)' not supported for '" + this + "'"); };

	@Override public String toString() { return wo.toString(); }
}