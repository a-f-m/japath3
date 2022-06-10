package japath3.core;

import japath3.core.Japath.Expr;
import japath3.core.Japath.NodeIter;

public final class Var<T> implements Expr {

	private Node node_;
	// for direct programmatic use (instead of via language expressions)  
	boolean preventClearing;
	
	public Var() {}

	private Var(Node node) { this.node_ = node; }
	
	public static Var of() { return new Var(null); }
	public static Var of(Node node) { return new Var(node); }
	
	@Override public NodeIter eval(Node node) {
//		if (bound()) throw new JapathException("var already bound: " + this);
		bindNode(node);
		return node == null ? Japath.empty : Japath.single(node);
	}
	
	public Node node() { return node_; }
	
	public Var<T> bindNode(Node node) {
		this.node_ = node;
		return this;
	}
	public T val() { return node_ == null ? null : (T) node_.val(); }

//	public T valClone() { return node_ == null ? null : (T) copy(node_.val()); }
	public T valClone() { return node_ == null ? null : (T) node_.woCopy(); }
	
	public boolean bound() { return node_ != null; }
	
	public Var<T> clear() {
		if (!preventClearing) node_ = null;
		return this;
	}
	
	@Override public void clearVars(Ctx ctx) { if (!ctx.preventClearing) clear(); }
	public Var<T> preventClearing(boolean preventClearing) { this.preventClearing = preventClearing; return this;}
	
	@Override public String toString() { return "^" + (node_ == null ? "null" : node_.toString()); }
}