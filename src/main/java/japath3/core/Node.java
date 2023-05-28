package japath3.core;

import static japath3.core.Japath.__;
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
import java.util.stream.Stream;

import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import japath3.core.Japath.Assignment;
import japath3.core.Japath.Assignment.Scope;
import japath3.core.Japath.Expr;
import japath3.core.Japath.NodeIter;
import japath3.core.Japath.NodeProcessing.Kind;
import japath3.core.Japath.PathExpr;
import japath3.core.Japath.Selection;
import japath3.schema.Schema;
import japath3.util.Basics.Ref;
import japath3.wrapper.WJsonOrg;

/**
 *
 * For efficiency reasons primitive java arrays and collections are used in parallel to the functional vavr lib. 
 * In order to be compact, 'single line'-code is preferred (similar to functional languages like scala).
 * 
 * @author andreas-fm
 *
 */
public abstract class Node extends NodeBase implements Cloneable {

	public static class DefaultNode extends Node {

		public DefaultNode(Object wo, Ctx ctx) {
			//!!!test
			//
			super(wo, "", null, ctx); 
		}
		//!!!test
		@SuppressWarnings("unused")
		private static Object xcheck(Object wo) {
			if ((wo instanceof JsonElement || wo instanceof JSONObject
					|| wo instanceof JSONArray) && wo != WJsonOrg.nullo && !(wo instanceof JsonNull) ) {
				System.out.println();
			}

			return wo;
			
		}

		@Override public NodeIter get(String name) {
			throw new UnsupportedOperationException();
		}
		@Override public NodeIter get(int i) {
			throw new UnsupportedOperationException();
		}
		@Override public NodeIter all(Object o) {
			throw new UnsupportedOperationException();
		}
		@Override public NodeIter desc() {
			throw new UnsupportedOperationException();
		};
		@Override public PrimitiveType type() {
			throw new UnsupportedOperationException();
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
		@Override public Node set(String name, Object o) { throw new UnsupportedOperationException(); }
		@Override public Node set(int idx, Object o) { throw new UnsupportedOperationException(); }
		@Override public void remove(Object selector) { throw new UnsupportedOperationException(); }
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
	
	final public Node create(Object x, Object selector) {
		return create(x, selector, this, ctx);
	}
	public Node create(Object wo, Object selector, Node previousNode, Ctx ctx) {
		throw new UnsupportedOperationException();
	};
	public Object createWo(boolean array) { throw new UnsupportedOperationException(); };
	public Object createWo(Object o) { throw new UnsupportedOperationException(); };
	final public NodeIter undef(Object sel) { return single(create(undefWo, sel, this, ctx)); }

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

	final private String selectionLog(Selection sel) { return " (selection: '" + stringify(sel, 0) + "'; node: '" + this + "')"; }
	
	final public NodeIter getChecked(String name, Selection sel) {
		Schema schema = ctx.getSchema();
		if (schema != null && schema.genCompleteness()) {
			schema.addPropHit(this, name);
		}
		return determRet(name, get(name), sel);
	}
	final public NodeIter getChecked(int i, Selection sel) {
		return determRet(i, get(i), sel);
	}

	public boolean isAttribute(String name) { return false; };
	public Node setNode(String name, Node n) { return set(name, n.val()); };
	public Node setNode(int idx, Node n) { return set(idx, n.val()); };
	public Node add(Object o) { return set(-1, o); };
	public Node addNode(Node n) { return add(n.val()); };
	public Iterator<String> childrenSelectors() { throw new UnsupportedOperationException(); }
	public NodeIter all() { return all(wo); };
	public NodeIter desc() { return descWalk(false); }
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
	public NodeIter text() { return Japath.singleObject(wo.toString(), previousNode); }
	final public boolean isCheckedLeaf() {
		if (wo == undefWo) throw new JapathException("operation on undef node '" + this + "' (assign value first)");
		else return isLeaf();
	}
	final public boolean isStruct() { return !isCheckedArray() && !isCheckedLeaf(); };
	final public boolean isCheckedArray() {
		if (wo == undefWo) throw new JapathException("operation on undef node '" + this + "' (assign value first)");
		else return isArray();
	}
	public boolean isNull() { throw new UnsupportedOperationException(); }
	public boolean isEmpty() {
		return !all().hasNext();
	}
	public int length() { throw new UnsupportedOperationException(); }
	public boolean containsWo(Object o) { throw new UnsupportedOperationException(); }
	public Object nullWo() { throw new UnsupportedOperationException(); }
	final public boolean hasIdxSelector() { return selector instanceof Integer; }	
	public boolean type(PrimitiveType t) { throw new UnsupportedOperationException(); };
	public PrimitiveType type() { throw new UnsupportedOperationException(); };

	final public void gatherDesc(List<Node> descs, Node node) {
		// TODO here we materialize all descendants. future work should iterate.
		descs.add(node);
		all(node.val()).forEachRemaining(x -> {
			gatherDesc(descs, x.setPreviousNode(node));
		});
	}
	
	public boolean exists(Object selector) {throw new UnsupportedOperationException(); }; // TODO use it in set
	final public Node seto(Object selector, Object o) { // TODO undef/overwritable?
		if (!construct) throw new JapathException("input node not modifyable: " + this);
		// TODO not for parents
//		else if (exists(selector)) throw new JapathException("'" + selector + "' already set: " + this);
		if (o == nullo) o = nullWo();
		return selector instanceof Integer i ? set(i, o) : set((String) selector, o);
	}
	final public Node node(String name) { return get(name).node(); };
	final public Node node(int i) { return get(i).node(); };
	public <T> T val(String name) { return get(name).val(); };
	final public <T> T val(String name, T d) { return get(name).val(d); };
	final public <T> T val(int i) { return get(i).val(); };
	final public <T> T val(int i, T d) { return get(i).val(d); };
	public <T> T val() {
		if (wo == undefWo) throw new JapathException("operation on undef node '" + this + "' (assign value first)");
		return (T) woVal(); 
	}
	public Object woVal() { return wo; }
	final public Node setCtx(Ctx ctx) {
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
	public void removeAll(String... props) {
		Japath.walki(this, Japath.desc).forEach(x -> {
			for (int i = 0; i < props.length; i++) x.remove(props[i]);
		});
	}
	public Node detach() {
		previousNode = null;
		return this;
	}
	final public Node setPreviousNode(Node n) {
		this.previousNode = n;
		return this;
	}
	final public Node setOrder(int order) {
		this.order = order;
		return this;
	}

	final public Node setConstruct(boolean construct) {
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
	public List<Node> nodePathToRoot() {
		List<Node> l = new ArrayList<Node>();
		toRoot(x -> {
			l.add(0, x);
		});
		return l;
	}
	public List<Object> selectorPath() {
		return selectorPathStream().collect(Collectors.toList());
	}
	private Stream<Object> selectorPathStream() {
		return nodePathToRoot().stream().map(x -> {
			return x.selector;
		});
	}
	public PathExpr selectorPathExpr() {
		return Japath.path(selectorPathStream().map(x -> {
			return x instanceof String name ? __(name)
					: x instanceof Integer idx ? __(idx) : null /* cannot happen */;
		}).toArray(Expr[]::new));
	}
	
	public Set<String> selectorNameSet() {
		
		Ref<Set<String>> ret = Ref.of(TreeSet.empty());
		walkr(this, (x, kind, __, __1, __2) -> {
			if (kind == Kind.Pre) {
				if (x.selector instanceof String) ret.r = ret.r.add(x.selector.toString());
			}
		});
		return ret.r;
		
	}
	
	public List<Node> leafNodes() { return leafNodes(false); }	
	public List<Node> leafNodes(boolean leafArrays) {
		
		List<Node> leafNodes = new ArrayList<>();
		Ref<Node> lastArr = new Ref<>();
				
		Japath.walkr(this, (x, kind, __, __1, __2) -> {
			if (kind == Kind.Pre && x.isLeaf()) {
				if (leafArrays && x.previousNode != null && x.previousNode.isArray()) {
					if (x.previousNode != lastArr.r) {
						// all have to be leafs
						NodeIter all = x.previousNode.all();
						for (Node y : all) {
							if (!y.isLeaf()) throw new JapathException("only leafs allowed");
						}
						//
						leafNodes.add(x.previousNode);
						lastArr.r = x.previousNode;
					}
				} else {
					leafNodes.add(x);
				}
			}
		});
		return leafNodes;
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


	final public <T> T v(String name) {
		
		return ctx.getVars().val(name);
	}
	
	final public void setAncestors0(Assignment assignment) {
		
		for (Node x: io.vavr.collection.List.ofAll(this.nodePathToRoot()).reverse()) {
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
	
	final public void setAncestors(Assignment assignment) {
		
		Node x = this;
		boolean undefs = false;
		int level = 0;
		while (x != null) {
			// recursion failure
			if (x != null && x != this && x.wo == this.wo) {
				throw new JapathException("resursion caused by " + (assignment == null ? "internal" : assignment));
			}
			if (!undefs && level == 1) // it is ensured that refs are up to date
				break;
			Node prev;
			if ((prev = x.previousNode) != null) {
				if (prev.wo == undefWo) {
					prev.wo = prev.createWo(x.hasIdxSelector());
					undefs = true;
				}
				prev.seto(x.selector, x.wo);
			}
			x = x.previousNode;
			level++;
		}
	}
	
	@Override public Object clone() throws CloneNotSupportedException { 

		Node clone = (Node) super.clone();
		clone.wo = woCopy();
		return clone; 
	}
	
	public String woString(int indent)  { throw new UnsupportedOperationException(); };

	@Override public String toString() { return wo.toString(); }
}