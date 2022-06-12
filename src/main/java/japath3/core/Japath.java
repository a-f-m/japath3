package japath3.core;

import static io.vavr.collection.List.ofAll;
import static japath3.core.Ctx.ParamAVarEnv.fromEnvx;
import static japath3.core.Japath.BoolExpr.Op.and;
import static japath3.core.Japath.BoolExpr.Op.not;
import static japath3.core.Japath.BoolExpr.Op.or;
import static japath3.core.Japath.Comparison.Op.eq;
import static japath3.core.Japath.Comparison.Op.ge;
import static japath3.core.Japath.Comparison.Op.gt;
import static japath3.core.Japath.Comparison.Op.le;
import static japath3.core.Japath.Comparison.Op.lt;
import static japath3.core.Japath.Comparison.Op.neq;
import static japath3.core.Node.nil;
import static japath3.core.Node.nilo;
import static japath3.core.Node.nullo;
import static japath3.util.Basics.embrace;
import static japath3.util.Basics.it;
import static japath3.util.Basics.stream;
import static java.util.Arrays.asList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.florianingerl.util.regex.Matcher;

import io.vavr.Tuple2;
import japath3.core.Ctx.ParamAVarEnv;
import japath3.core.Japath.NodeProcessing.Kind;
import japath3.core.Node.DefaultNode;
import japath3.core.Node.PrimitiveType;
import japath3.util.Basics;
import japath3.util.Regex;

/**
 * core containing ADT and 'select & walking'. In order to be compact, 'single line'-code is preferred (similar to functional languages).
 * Furthermore, as usual in languages like scala, relevant (ADT-)classes are defined within this class. 
 * For efficiency reasons primitive java arrays and collections are used in parallel to the functional vavr lib.
 * 
 * Note that the code is highly optimized concerning verbosity and usage of the java programming language. Therefore "classical" code 
 * quality tools might pop-up suggestions. 
 *   
 * @author andreas-fm
 *
 */
public class Japath {

	//----------------- adt ------------------------

	public static abstract class NodeIter implements Iterator<Node>, Iterable<Node> {

		public <T> T val() {
			T val = val(null);
			if (val == null) throw new JapathException("empty node iter");
			else return val;
		};
		public <T> T val(T d) { T val = node().val(); return val == nilo ? d : val; };
		public Node node() { return hasNext() ? next() : nil;};
		
		public boolean arrayFlag() { return false; }
		@Override public Iterator<Node> iterator() { return this; }
	}
	public static NodeIter single(Node node) {
		return new NodeIter() {
			
			boolean consumed;
			
			@Override public boolean hasNext() { return !consumed; }
			@Override public Node next() { consumed = true; return node; }
		};
	}
	public static NodeIter nodeIter(Iterator<Node> nodes) {
		return new NodeIter() {
			
			@Override public boolean hasNext() { return nodes.hasNext(); }
			@Override public Node next() { return nodes.next(); }
		};
	}
	public static NodeIter nodeIter(NodeIter nit, Runnable finish) {
		return new NodeIter() {
			boolean finished;
			@Override public boolean hasNext() {
				boolean hasNext = nit.hasNext();
				if (finished && hasNext) throw new JapathException("irregular use of parametric expression");
				if (!hasNext && !finished) finish.run();
				finished = !hasNext;
				return hasNext;
			}
			@Override public Node next() { return nit.next(); }
		};
	}
	
	public static NodeIter empty = new NodeIter() {
		@Override public boolean hasNext() {return false;}
		@Override public Node next() { throw new UnsupportedOperationException(); }
	};
	// for 'opt'-use in 'testIt'
	public static NodeIter emptyTrueCut = new NodeIter() {
		@Override public boolean hasNext() {return false;}
		@Override public Node next() { throw new UnsupportedOperationException(); }
	};
	public static NodeIter ok = singleBool(true, nil);

	@FunctionalInterface
	public static interface Expr {
		public NodeIter eval(Node node, Object... envx);
		public default void clearVars(Ctx ctx) {};
		public default Stream<Expr> stream() {throw new UnsupportedOperationException();};
		public default void visit(BiConsumer<Expr, Boolean> c) {c.accept(this, true);};
		public static Expr Nop = new Expr() {
			@Override public NodeIter eval(Node node, Object... envx) { return single(node); }
			@Override public String toString() { return "Nop"; }
		};	
		public default Expr deepCopy(Expr[] params, Map<String, Bind> vmap) throws CloneNotSupportedException {return this;};
		public default Expr paramClone(Ctx.ParamAVarEnv env) throws CloneNotSupportedException {return this;};
	}
	
	public static abstract class AExpr implements Expr, Cloneable {
	}

	/** usually, these are lisp-like expressions */
	public static abstract class CompoundExpr extends AExpr {
		public Expr[] exprs = new Expr[0];
		@Override public Stream<Expr> stream() { return Arrays.stream(exprs); }
		@Override public void clearVars(Ctx ctx) { for (Expr expr : exprs) expr.clearVars(ctx); }
		@Override public String toString() { return strStream(); }
		protected String strStream() {
			return strStream(exprs);
		}
		protected String strStream(Expr... exprs) {
			return "(" +  Arrays.stream(exprs).map(x -> {
				return x.toString();
			}).collect(Collectors.joining(",")) + ")";
		}
		@Override public void visit(BiConsumer<Expr, Boolean> c) { 
			c.accept(this, true);
			for (Expr e : exprs) e.visit(c); 
			c.accept(this, false);
		}
		public Expr last() {return exprs[exprs.length - 1];};
		@Override public Expr deepCopy(Expr[] params, Map<String, Bind> vmap) throws CloneNotSupportedException { 
			CompoundExpr clone = (CompoundExpr) super.clone();
			clone.exprs = new Expr[exprs.length];
			for (int i = 0; i < exprs.length; i++) {
				clone.exprs[i] = exprs[i].deepCopy(params, vmap);
			}
			return clone; 
		}
		@Override
		public Expr paramClone(ParamAVarEnv env) throws CloneNotSupportedException {
			CompoundExpr clone = (CompoundExpr) super.clone();
			clone.exprs = new Expr[exprs.length];
			for (int i = 0; i < exprs.length; i++) {
				clone.exprs[i] = exprs[i].paramClone(env);
			}
			return clone; 
		}
	}
	
	public static Expr srex(String regex) {
		return (y, envx) -> {
			return y.selector.toString().matches(regex) ? single(y) : empty;
		};
	}
	public static <T> Expr bind(Var<T> v) {
		return bind(null, v);
	}
	public static <T> Expr bind(Expr e, Var<T> v) {
		
		return new Expr() {
			
			@Override public NodeIter eval(Node y, Object... envx) { 
				if (e == null) {
					v.bindNode(y);
				} else {
					NodeIter nit = e.eval(y, envx);
					if (nit.hasNext()) {
//						if (nit.hasNext()) throw new JapathException("not allowed"); TOOD
						v.bindNode(nit.next());
					} else {
						v.clear();
					}
				}
				return single(y);
			}
			@Override public String toString() { return "bind(" + e + ", " + v + ")"; }
		};
	}
	
	public static class Bind extends AExpr {
		
		public String vname;
		boolean isLocalCanditate;
		
		// set only run time after clone iff isLocal
		Var var;

		public Bind(String vname) { this.vname = vname; }

		@Override public NodeIter eval(Node node, Object... envx) {
			
			String vname_ = vname.equals("_") ? node.selector.toString() : vname;
			
			Vars globalVars = node.ctx.getVars();
			Var globalVar = globalVars.getVar(vname_);

			if (globalVar == null && isLocalCanditate) {
				
//		deepCopy version:				
//				var.bindNode(node);
//				return single(node);
				
				Var var_ = fromEnvx(envx).registerVar(vname_);
				var_.bindNode(node);
				return single(node);
			}
			
			// legacy global vars old style for compatibility:
			
			@SuppressWarnings("unused")
			boolean firstOcc;
			if (globalVar == null) {
				
				Tuple2<Var, Boolean> reg = globalVars.register(vname_, this);
				globalVar = reg._1;
				firstOcc = true;
			} 
			else {
				firstOcc = globalVars.firstOcc(vname_, this);
			}
			// testing re-write var
//			if (firstOcc) {
				globalVar.bindNode(node);
				return single(node);
//			} else {
//				throw new JapathException("var '" + vname_ + "' already defined");
////				return node.val().equals(var.val()) ? single(node) : empty;
//			}
		}
		@Override
		public void clearVars(Ctx ctx) {
			if (ctx == null) throw new JapathException();
			Vars vars = ctx.getVars();
			Var var = vars.getVar(vname);
			if (var != null && vars.firstOcc(vname, this)) {
				var.clearVars(ctx);
			}
		}
		
		public void connectLocalVar(Map<String, Bind> vmap, String defName) {
			if (vname.equals("$")) return;
			isLocalCanditate = true;
			if (vmap.putIfAbsent(vname, this) != null)
				throw new JapathException("variable '" + vname + "' already assigned within def '" + defName + "'");
		}
		@Override public Expr deepCopy(Expr[] params, Map<String, Bind> vmap) throws CloneNotSupportedException {
			if (isLocalCanditate) {
				Bind clone = (Bind) super.clone();
				clone.var = new Var();
				vmap.put(vname, clone);
				return clone; 
			} else {
				return this;
			}
		}

		@Override public String toString() { return "bind_(" + embrace(vname, '"') + ")"; }
	}
	
	public static class VarAppl extends AExpr {

		public String vname;
		
		// only for local vars used to point to var-def (1st occ)
		public Bind def;

		public VarAppl(String vname) { this.vname = vname; }

		@Override public NodeIter eval(Node node, Object... envx) {
//			deepCopy version:			
//			Var var = def != null ? def.var : node.ctx.getVars().v(vname);
			
			Var var = def != null ? fromEnvx(envx).getVar(vname) : node.ctx.getVars().v(vname);

			// play save:
			if (!var.bound()) throw new JapathException("var '" + vname + "' not bound");
			
			return single(var.node());
		}
		
		public void connectLocalVar(Map<String, Bind> vmap, String defName) {
			if (vname.equals("$")) return;
			Bind def = vmap.get(vname);
			if (def != null) {
				this.def = def;
			} else {
				// global var
//				throw new JapathException("variable '" + vname + "' not assigned before within def '" + defName + "'");
			}	
		}
		
		@Override public Expr deepCopy(Expr[] params, Map<String, Bind> vmap) throws CloneNotSupportedException {
			VarAppl clone = (VarAppl) super.clone();
			Bind bind = vmap.get(vname);
			if (bind != null) {
				clone.def = bind;
				return clone;
			} else {
				// then it's a global var
				return this;
			}
		}

		@Override public String toString() { return "varAppl(" + embrace(vname, '"') + ")"; }
	}
	
	public static class ParametricExprDef extends CompoundExpr {
		
		public String name;
		
		public ParametricExprDef(String name, Expr e) {
			this.name = name;
			exprs = new Expr[] { e };
			Map<String, Bind> vmap = new HashMap<>();
			e.visit((x, pre) -> {
				if (x instanceof Bind) ((Bind) x).connectLocalVar(vmap, name);
				if (x instanceof VarAppl) ((VarAppl) x).connectLocalVar(vmap, name); 
			});

		}
		@Override public NodeIter eval(Node node, Object... envx) { node.ctx.declare(name, this); return single(node); }
		@Override public String toString() { return "def('" + name + "', " + exprs[0] + ")"; }
	}
	
	public static class ParametricExprAppl extends CompoundExpr {
		
		public String name;
		
		public ParametricExprAppl(String name, Expr[] exprs) {
			this.name = name;
			this.exprs = exprs == null ? new Expr[0] : exprs;
		}
		@Override public NodeIter eval(Node node, Object... envx) {
			
			try {
				ParametricExprDef ped = node.ctx.getPed(name);
//				deepCopy version:				
//				return ped.exprs[0].deepCopy(exprs, new HashMap<String, Japath.Bind>()).eval(node, envx);
				
				
				return ped.exprs[0].eval(node, new Ctx.ParamAVarEnv().cloneResolvedParams(exprs, envx));
				
			} catch (Exception e) {
				throw new JapathException(e);
			}
		}
		
		@Override public String toString() { return name + strStream(); }
	}
	
	public static class ParamAppl extends AExpr {
		
		public int i;
		
		public ParamAppl(int idx) { this.i = idx; }
		
		@Override public NodeIter eval(Node node, Object... envx) {
			
			
			ParamAVarEnv env = fromEnvx(envx);
			if (i < 0 || i >= env.params.length) throw new JapathException(
					"bad (zero-based) parameter number " + i + " (parameter expressions: " + asList(env.params) + ")");
			return env.params[i].eval(node, envx);
//			throw new JapathException("cannot happen");

		}
		@Override public Expr deepCopy(Expr[] params, Map<String, Bind> vmap) throws CloneNotSupportedException {
			
			if ( i < 0 || i >=  params.length) {
				throw new JapathException(
						"bad (zero-based) parameter number " + i + " (parameter expressions: " + asList(params) + ")");
			} else {
				return params[i];
			}
		}
		@Override
		public Expr paramClone(ParamAVarEnv env) throws CloneNotSupportedException {
			if (env == null) throw new JapathException("no parameter given (expr: '" + this + "')");
			if (env.params.length == 0) throw new JapathException("parameter '" + this + "' not bound");
			if (i < 0 || i >= env.params.length) throw new JapathException(
					"bad (zero-based) parameter number " + i + " (parameter expressions: " + asList(env.params) + ")");
			return env.params[i];
		}
		@Override public String toString() { return "#" + i; }
	}
	
	public static Expr all = new All();
	public static class All extends AExpr {
		@Override public NodeIter eval(Node node, Object... envx) { return node.all(); }
		@Override public String toString() { return "all" ; }
	}

	public static Expr desc = new Desc();
	public static class Desc extends AExpr {
		@Override public NodeIter eval(Node node, Object... envx) { return node.desc(); }
		@Override public String toString() { return "desc" ; }
	}
	
	public static Expr self = new Self();
	public static class Self extends AExpr {
		@Override public NodeIter eval(Node node, Object... envx) { return single(node); }
		@Override public String toString() { return "self" ; }
	}
	
	public static Expr create = new Create();
	public static class Create extends AExpr {
		@Override public NodeIter eval(Node node, Object... envx) {
			return single(node.create(Node.undefWo, "", null, node.ctx).setConstruct(true));
		}
		@Override public String toString() { return "create" ; }
	}

	public static Expr text = new Text();
	public static class Text extends AExpr {
		@Override public NodeIter eval(Node node, Object... envx) { return node.text(); }
		@Override public String toString() { return "text()" ; }
	}
	
	// classical path steps according to xpath formal semantics
	public static class PathExpr extends CompoundExpr {

		private PathExpr(Expr[] exprs) {
			this.exprs = exprs;
		}
		@Override
		public NodeIter eval(Node node, Object... envx) {
			return eval(exprs, node, envx);
		}
		private NodeIter eval(Expr[] steps, Node node, Object... envx) {
			if (steps == null || steps.length == 0) {
				return single(node);
			} else {
				NodeIter itY = steps[0].eval(node, envx);
//				if (itY == emptyTrue) return singleBool(true);
				if (itY == emptyTrueCut) return itY;
				return new NodeIter() {
					NodeIter itZ = empty;
					@Override
					public boolean hasNext() {
						
						loop: while(true) {
							
							if (!itZ.hasNext()) {
								if (itY.hasNext()) {
									Expr[] tail = steps.length == 1 ? null : Arrays.copyOfRange(steps, 1, steps.length);
									if (tail != null) for (Expr e : tail) e.clearVars(node.ctx);
									Node next = itY.next();
//									System.out.println(">>> steps: " + asList(steps));
//									System.out.println(">>> tail: " + (tail != null ? asList(tail) : "null"));
//									System.out.println(">>> next: " + next);
									itZ = eval(tail, next, envx);
									if (itZ.hasNext()) {
										return true;
									} else {
										continue loop;
									}
								} else {
									return false;
								}
							} else {
								return true;
							}
						}
					}
					@Override
					public Node next() {
						return itZ.next();
					}
				};
			}
		}
		
		public PathExpr revTail() {return path(Arrays.copyOfRange(exprs, 0, exprs.length - 1));}
		
		@Override public String toString() { return "path" + strStream(); }
		
	}
	
	public static class Walk extends CompoundExpr {
		private Walk(Expr[] exprs) { this.exprs = exprs; }
		@Override public NodeIter eval(Node node, Object... envx) {
			for (@SuppressWarnings("unused") Node n : walki(node, exprs));
			return single(node);
		}
		@Override public String toString() { return "Walk" + asList(exprs) ; }
	}

	public static class Property extends Selection {

		public String name;
		public boolean isTrueRegex;
		public boolean ignoreRegex;
		
		private Property(String name, boolean ignoreRegex) {
			
			this.name = name;
			this.ignoreRegex = ignoreRegex;
//			if (!Language.isIdentifier(name) && (isTrueRegex = Regex.isTrueRegex(name))) {
			if (isTrueRegex = Regex.isTrueRegex(name)) {
				String expl = Regex.check(name);
				if (expl != null) throw new JapathException("'" + name + "' is not a regex (" + expl + ")");
			}
		}
		public NodeIter eval(Node node, Object... envx) {
			
			if (isTrueRegex && !ignoreRegex) {
				return regexSelection(node);
			} else {
				// TODO better attribute handling
				if (!node.isAttribute(name)) node.ctx.checkName(name);
				//
				return node.getChecked(name, this);
			}
		}
		private NodeIter regexSelection(Node node) {
			
			if (node.isCheckedArray()) throw new JapathException("no selector regex match allowed at arrays");
			io.vavr.collection.Iterator<String> matchedSelectors =
					io.vavr.collection.Iterator.ofAll(node.childrenSelectors()).filter(sel -> {
						return sel.matches(name);
					});
			Selection sel = this;
			return nodeIter(new io.vavr.collection.Iterator<NodeIter>() {
				@Override public boolean hasNext() { return matchedSelectors.hasNext(); }
				@Override public NodeIter next() { return node.getChecked(matchedSelectors.next(), sel); }
			}.flatMap(nit -> {
				return it(nit);
			}));
		}
		@Override public boolean isProperty() {return true;}
		@Override public Object selector() { return name; }
		@Override public String toString() { return "__" + (isTrueRegex ? "r" : "") + (ignoreRegex ? "i" : "")
				+ "(" + embrace(name, '"').replace("\\`", "`") + ")"; }
	}
	
	public static class PathAsProperty extends Selection {

		public Expr expr;

		public PathAsProperty(Expr e) { expr = e; }

		@Override public NodeIter eval(Node node, Object... envx) {

			NodeIter nit = expr.eval(node, envx);
			
			if (nit.hasNext()) {
				if (nit.hasNext()) {
					new JapathException("single evaluation item expected at node '" + node + "' (expr: " + expr + ")");
				}
				Object s = nit.next().val();
				if (!(s instanceof String)) {
					throw new JapathException(
							"evaluation to string expected, found '" + s + "' (at node '" + node + "', expr: " + expr + ")");
				} else {
					return node.getChecked((String) s, this);
				}
			} else {
				throw new JapathException("non-empty evaluation expected at node '" + node + "' (expr: " + expr + ")");
			}
		}		
		
		@Override public Expr deepCopy(Expr[] params, Map<String, Bind> vmap) throws CloneNotSupportedException { 
			PathAsProperty clone = (PathAsProperty) super.clone();
			clone.expr = expr.deepCopy(params, vmap);
			return clone;
		}
		@Override public Expr paramClone(Ctx.ParamAVarEnv env) throws CloneNotSupportedException { 
			PathAsProperty clone = (PathAsProperty) super.clone();
			clone.expr = expr.paramClone(env);
			return clone;
		}
		
		
		@Override public void visit(BiConsumer<Expr, Boolean> c) {
			c.accept(this, true);
			expr.visit(c); 
			c.accept(this, false);
		};

		@Override public boolean isProperty() { return true; }
		@Override public Object selector() { throw new JapathException("dynamic selector"); }
		@Override public String toString() { return "pathAsPropery(" + expr + ")"; }
	}
	
	public static class Idx extends Selection {

		public int i;
		public boolean seq;
		// only for seq i..upper, -1 is length, null is no-slice
		public Integer upper;
		
		private Idx(int i, Integer upper, boolean seq) {
			this.i = i;
			this.upper = upper;
			this.seq = seq;
		}
		public NodeIter eval(Node node, Object... envx) {
			
			if (seq) {
				if (node.construct) throw new JapathException("construction not allowd for sequence subscript");
				return node.order >= i && node.order <= (upper == null ? i : upper == -1 ? Integer.MAX_VALUE : upper)
						? single(node)
						: empty;
			} else {
				NodeIter e = node.getChecked(i, this);
				return e == empty && node.construct ? node.undef(i) : e;
			}
		}
		@Override public boolean isProperty() { return false; }
		@Override public Object selector() { return i; }
		@Override public String toString() { return "__(" + i + (seq ? ", true" : "") + ", " + upper + "" + ")"; }
	}
	
	public static abstract class Selection extends AExpr {
		protected Assignment.Scope scope = Assignment.Scope.none;
		public abstract boolean isProperty();
		public abstract Object selector();
		public static Expr expr(Object selector) {
			return selector instanceof String ? (((String) selector).equals("*") ? all : __((String) selector))
					: selector instanceof Integer ? __((Integer) selector) : null;
		}
	}
	
	public static Expr sel = new Selector();
	public static class Selector extends AExpr {

		@Override public NodeIter eval(Node node, Object... envx) { return single(new DefaultNode(node.selector.toString(), node.ctx)); }
		@Override public String toString() { return "sel"; }
	}
	
	// for compactness:
	public static <T> Expr c_(T o) {
		return constExpr(o);
	}
	public static <T> Expr constExpr(T o) {
		return new Expr() {
			
			@Override public NodeIter eval(Node node, Object... envx) { 
				return singleObject(o, node); }
			@Override public String toString() {
				return o instanceof String ? embrace(((String) o).replace("'", "\\'"), '\'') : o.toString();
			}
		};
	}
	
	public static class Comparison<T> extends CompoundExpr {
		public enum Op {eq, neq, lt, gt, le, ge, match};
		public Op op;
		
		private Comparison(Op op, Expr e) { this.op = op; exprs = new Expr[] { e }; }

		@Override public NodeIter eval(Node node, Object... envx) {
			
			boolean ret = false;
			NodeIter nit = exprs[0].eval(node, envx);
			if (nit.hasNext()) {
				
				Object o = nit.val();
				Object v = node.val();
				
				if (op == eq || op == neq || op == lt || op == gt || op == le || op == ge) {
					
					// type coercion for numbers
					if (o instanceof Number) o = ((Number) o).doubleValue();
					if (v instanceof Number) v = ((Number) v).doubleValue();
					//
					checkVals(node, o, v);
						
					int cmp = o == nullo ? (v == node.nullWo() ? 0 : -1) : ((Comparable<Object>) v).compareTo(o);
					ret = op == lt ? cmp < 0 //
							: op == gt ? cmp > 0 //
									: op == le ? cmp <= 0 //
									: op == ge ? cmp >= 0 //
									: op == eq ? cmp == 0 //
									: op == neq ? cmp != 0 //
									: false
									;
				} else { // regex
					Matcher m = Regex.match(o.toString(), v.toString());
					String g = Regex.group(m, 1);
					return m != null ? (g != null ? singleObject(g, node) : singleBool(true, node)) : singleBool(false, node);
				}
			}
			return singleBool(ret, node); 
		}
		private void checkVals(Node node, Object o, Object v) {
			
			if (o != nullo && !v.getClass().equals(o.getClass())) { 
				throw new JapathException(
						"'" + v + "' and '" + o + "' must be of same class (found '"
					+ v.getClass() + "', '"
					+ o.getClass() + "'; no type coercion yet)");
			}
			if (o != nullo && !(v instanceof Comparable))
				throw new JapathException("'" + v.getClass() + "' is not instance of 'Comparable' at '" + this + "'");
			
			if (!(op == eq || op == neq) && (o == node.nullWo() || v == node.nullWo())) 
				throw new JapathException("null not comparable at '" + this + "'");
		}
		@Override public String toString() { return op + "(" + exprs[0] + ")"; }
	}
	
	public static class ExternalCall extends CompoundExpr {
		
		public String kind;
		public String ns;
		public String func;
		
		private ExternalCall(String kind, String ns, String func, Expr[] exprs) {
			this.kind = kind;
			this.ns = ns;
			this.func = func;
			this.exprs = exprs == null ? new Expr[0] : exprs;
		}
		public NodeIter eval(Node node, Object... envx) {
			
			NodeIter[] nits = new NodeIter[exprs.length];
			for (int i = 0; i < nits.length; i++) nits[i] = exprs[i].eval(node, envx);
			if (kind.equals("directive")) {
				return node.ctx.handleDirective(ns, func, node, nits);
			} else if (kind.equals("java")) {
				return Ctx.invoke(ns, func, node, nits);
			} else if (kind.equals("javascript")) {
				
				return Ctx.invokeJs(func, node, nits);
			} else {
				throw new JapathException("'" + kind + "' not supported at '" + this + "'");
			}
		}
		@Override public String toString() { return "externalCall[" + kind + ", " + ns + ", " + func + ", " + asList(exprs) + "]"; }
	}
	

	public static class HasType extends AExpr {
		
		public PrimitiveType t;		
		
		public HasType(PrimitiveType t) { this.t = t; }
//		public HasType(String t) { this.t = PrimitiveType.valueOf(t); }
		@Override public NodeIter eval(Node node, Object... envx) { 
			return singleBool(node.type(t), node); 
			}
		@Override public String toString() { return "type(" + t + ")"; }
	}
	
	public static class Union extends CompoundExpr {
		
		private Union(Expr[] exprs) { this.exprs = exprs; }

		@Override public NodeIter eval(Node node, Object... envx) {
			
			Iterable<Node>[] iters = new Iterable[exprs.length * 2];
			for (int i = 0, j = 0; i < iters.length; i += 2, j++) {
				Expr ei = exprs[j];
				iters[i] = it(ei.eval(node, envx));
				iters[i + 1] = it(new Iterator<Node>() {
					@Override public boolean hasNext() {
						ei.clearVars(node.ctx);
						return false;
					}
					@Override public Node next() { throw new UnsupportedOperationException(); }
				});
			}
			io.vavr.collection.Iterator<Node> c = io.vavr.collection.Iterator.concat(iters);
			return new NodeIter() {
				@Override public Node next() { return c.next(); }
				@Override public boolean hasNext() { return c.hasNext(); }
			};
		}
		
		@Override public String toString() { return "union" + strStream(); }
	}
	
	public static class Optional extends CompoundExpr {
		
		private Optional(Expr... opt) { exprs = opt; }
		
		@Override public NodeIter eval(Node node, Object... envx) {
			
			NodeIter nit = exprs[0].eval(node, envx);
			return nit.hasNext() ? nit : emptyTrueCut;
		}
		@Override public String toString() { return "opt(" + exprs[0] + ")"; }
	}
	
	public static class BoolExpr extends CompoundExpr {
		
		public static enum Op {and, or, xor, not, imply, constant};
		public Op op;
		
		// pub for special.
		protected BoolExpr(Op op, Expr[] exprs) {
			this.exprs = exprs;
			this.op = op;
		}
		public NodeIter eval(Node node, Object... envx) {
			
			boolean b = true;
			switch (op) {
			case and:
			case not:
				b = true;
				for (Expr e : exprs) 
					b = testIt(e.eval(node, envx)) && b;
				break;
			case or:
			case xor:
				b = false;
				for (Expr e : exprs) { 
					boolean test = testIt(e.eval(node, envx));
					b = op == or ? test || b : test ^ b;
				}
				break;
			case imply:
				b = testIt(exprs[0].eval(node, envx)) ? testIt(exprs[1].eval(node, envx)) : true;
				break;
			case constant:
				return exprs[0].eval(node, envx);
			default: throw new UnsupportedOperationException();
			}
			return singleBool( op == not ? !b : b, node);
		}

		public static Expr True = new Expr() {
			@Override public NodeIter eval(Node node, Object... envx) { return singleBool(true, node); }
			@Override public String toString() { return "true"; }
		};
		public static Expr False = new Expr() {
			@Override public NodeIter eval(Node node, Object... envx) { return singleBool(false, node); }
			@Override public String toString() { return "false"; }
		};
		@Override public String toString() { return (op != Op.constant ? op.toString() + strStream() : exprs[0].toString() ); }
	}
	
	public static class SubExpr extends CompoundExpr {
		
		protected SubExpr(boolean passNode, Expr[] exprs) { this.exprs = exprs; }
		
		@Override public NodeIter eval(Node node, Object... envx) {
			Node x = node;
			for (Expr e : exprs)
				e.eval(x, envx).forEachRemaining(dummy -> {});
			
			return single(x);
		}
		
		@Override public String toString() { return "subExpr" + strStream(); }
	}
	
	public static class Struct extends CompoundExpr {
		
		protected Struct(Expr[] exprs) { this.exprs = exprs; }
		
		@Override public NodeIter eval(Node node, Object... envx) {
//			Node x = node.create(Node.undefWo, "", null, node.ctx).setConstruct(true);
			Node x = node.create(node.createWo(false), "", null, node.ctx).setConstruct(true);
			for (Expr e : exprs) {
				if (!(e instanceof Assignment))
					throw new JapathException("only assignments allowed at " + this);
				((Assignment) e).assignEval(x, node, envx);
			}
			return single(x);
		}
		
		@Override public String toString() { return "struct" + strStream(); }
	}
	
	public static class Array extends CompoundExpr {
		
		protected Array(Expr[] exprs) { this.exprs = exprs; }
		
		@Override public NodeIter eval(Node node, Object... envx) {
			Node x = node.create(node.createWo(true), "", null, node.ctx).setConstruct(true);
			int i = 0;
			for (Expr e : exprs) {
				for (Node n : e.eval(node, envx)) {
					x.set(i, n.val());
					i++;
				}
			}
			return single(x);
		}
		
		@Override public String toString() { return "array" + strStream(); }
	}
	
	public static class Cond extends CompoundExpr {
		
		private Cond(Expr b, Expr ifExpr, Expr elseExpr) {
			exprs = new Expr[] { b, ifExpr, elseExpr == null ? Nop : elseExpr };
		}
		
		@Override public NodeIter eval(Node node, Object... envx) {
			return testIt(exprs[0].eval(node, envx)) ? exprs[1].eval(node, envx)
					: exprs[2] == Nop ? empty : exprs[2].eval(node, envx);
		}
		
		@Override public String toString() {
			return "cond(" + exprs[0] + ", " + exprs[1] + (exprs[2] == Nop ? "" : ", " + exprs[2]) + ")";
		}
	}
	
	public static class QuantifierExpr extends CompoundExpr {
		
		public static enum Op {every, some};
		public Op op;
				
		protected QuantifierExpr(Op op, Expr qant, Expr check) {
			this.op = op;
			exprs = new Expr[] { qant == null ? all : qant, check };
		}

		@Override public NodeIter eval(Node node, Object... envx) {
			
			NodeIter nit = exprs[0].eval(node, envx);
			boolean b = op == Op.every; // acc. to xpath sem, false if 'some'
			while (nit.hasNext()) {
				boolean t = testIt(exprs[1].eval(nit.next(), envx));
				b = op == Op.every ? t && b : t || b;
			}
			return singleBool(b, node);
		}

		@Override public String toString() { return op + "(" + (exprs[0] instanceof All ? "" : exprs[0] + ", ") + exprs[1] + ")"; }
	}
	
	public static class Filter extends CompoundExpr {

		private Filter(Expr expr) { exprs = new Expr[] { expr }; }
		@Override public NodeIter eval(Node node, Object... envx) { return testIt(exprs[0].eval(node, envx)) ? single(node) : empty; }
		@Override public String toString() { return "?filter(" + exprs[0] + ")"; }
	}
	
	public static class Assignment extends CompoundExpr {
		
		public static enum Scope { lhs, rhs, none }
		
		private Assignment(Expr lhs, Expr rhs) { 
			exprs = new Expr[] { lhs, rhs };
			lhs.visit((x, pre) -> {
				setScope(x, Scope.lhs);
			});
			rhs.visit((x, pre) -> {
				setScope(x, Scope.rhs);
			});
		}

		private void setScope(Expr x, Scope scope) {
			if (x instanceof Selection && ((Selection) x).scope == Scope.none) 
				((Selection) x).scope = scope;
		}
		
		public NodeIter assignEval(Node lhsCtxNode, Node rhsCtxNode, Object... envx) {

			List<Node> ret = new ArrayList<>();
			
			NodeIter nit = exprs[1].eval(rhsCtxNode, envx);
			io.vavr.collection.List<Node> nl = ofAll(nit);
			boolean single = nl.size() == 1;
			
			Expr expr = exprs[0];
			// if a var treat it as a selection
//			if (expr instanceof VarAppl) {
//				expr = __(expr.eval(lhsCtxNode).val().toString());
//			}
			//
			NodeIter e0 = expr.eval(lhsCtxNode, envx);
			e0.forEachRemaining(lhNode -> {
				
				int i = 0;
				for (Node n : nl) {
					if (single && !nit.arrayFlag()) {
//						lhNode.wo = n.woCopy(); re-think
						lhNode.wo = n.woVal();
						lhNode.setAncestors(this);
					} else {
//						lhNode.create(n.woCopy(), i, lhNode, lhNode.ctx).setAncestors(this); re-think
						lhNode.create(n.woVal(), i, lhNode, lhNode.ctx).setAncestors(this);
					}
					i++;
				}
				ret.add(lhNode);
			});
			return nodeIter(ret.iterator());
		}
		
		@Override public NodeIter eval(Node node, Object... envx) { 
			return assignEval(node, node, envx);
		}

		@Override public String toString() { return "assign(" + exprs[0] + ", " + exprs[1] + ")"; }
	}
	
	public static class Message extends CompoundExpr {
		
		private Message(Expr... opt) { exprs = opt; }
		
		@Override public NodeIter eval(Node node, Object... envx) {
			
			System.out.println(getMessage(node, envx));
			return single(node);
		}
		public String getMessage(Node node, Object... envx) { return exprs[0].eval(node, envx).node().val().toString(); }
		@Override public String toString() { return "message(" + exprs[0] + ")"; }
	}
	
	public static NodeIter singleBool(boolean b, Node scopeNode) {
		
		return single(new DefaultNode(b, scopeNode.ctx) {
			@Override public PrimitiveType type() { return PrimitiveType.Boolean; }
		});
	}
	public static NodeIter singleObject(Object o, Node scopeNode) {
		if (o == null) throw new JapathException("null not possible (scope: " + scopeNode + ")");
		return single(new DefaultNode(o, scopeNode.ctx) {
			@Override public PrimitiveType type() { return PrimitiveType.Any; }
		});
	}	

	//----------------- end adt ------------------------
	
	//----------------- constructor methods (to be used in japath java expressions) in scala it would be implicit in class head ;-) -------
	
	public static Bind bind_(String vname) { return new Bind(vname); }
	public static VarAppl varAppl(String vname) { return new VarAppl(vname); }
	public static ParametricExprDef paramExprDef(String name, Expr e) { return new ParametricExprDef(name, e); }
	public static ParametricExprAppl paramExprAppl(String name, Expr... exprs) { return new ParametricExprAppl(name, exprs); }
	public static ParamAppl paramAppl(int i) { return new ParamAppl(i); }
	public static PathExpr path(Expr... exprs) { return new PathExpr(exprs); }
	// for compactness:
	public static PathExpr p_(Expr... exprs) { return new PathExpr(exprs); }
	public static Walk walk(Expr... exprs) { return new Walk(exprs); }
	public static Expr __(String... names) {
		return __(false, names);
	}
	public static Expr __(boolean ignoreRegex, String... names) {
		if (names.length == 1) {
			return new Property(names[0], ignoreRegex);
		} else {
			PathExpr path = path(new Expr[names.length]);
			for (int i = 0; i < names.length; i++) {
				path.exprs[i] = new Property(names[i], ignoreRegex);
			}
			return path;
		}
	}
	public static PathAsProperty pathAsProperty(Expr expr) { return new PathAsProperty(expr); }
	public static Idx __(int i) { return __(i, false); }
	public static Idx __(int i, boolean seq) { return new Idx(i, null, seq); }
	public static Idx __(int i, Integer upper, boolean seq) { return new Idx(i, upper, seq); }
	public static <T> Comparison<T> cmpConst(Comparison.Op op, T o) { return new Comparison<T>(op, constExpr(o)); }
	public static <T> Comparison<T> cmp(Comparison.Op op, Expr expr) { return new Comparison<T>(op, expr); }
	public static <T> Comparison<T> eq(T o) { return cmpConst(eq, o); }
	public static <T> Comparison<T> neq(T o) { return cmpConst(neq, o); }
	public static <T> Expr externalCall(String kind, String ns, String func, Expr[] exprs) { return new ExternalCall(kind, ns, func, exprs); }
	public static HasType type(PrimitiveType t) { return new HasType(t); }
	public static HasType type(String t) { return type(PrimitiveType.valueOf(t)); }
	public static Union union(Expr... exprs) { return new Union(exprs); }
	public static Optional optional(Expr expr) { return new Optional(expr); }
	public static Optional opt(Expr expr) { return optional(expr); }
	public static BoolExpr boolExpr(BoolExpr.Op op, Expr... exprs) { return new BoolExpr(op, exprs); }
	public static SubExpr subExpr(boolean passNode, Expr... exprs) { return new SubExpr(passNode, exprs); }
	public static SubExpr subExpr(Expr... exprs) { return new SubExpr(true, exprs); }
	public static Struct struct(Expr[] exprs) { return new Struct(exprs); }
	public static Array array(Expr[] exprs) { return new Array(exprs); }
	public static BoolExpr and(Expr... exprs) { return boolExpr(and, exprs); }
	public static BoolExpr or(Expr... exprs) { return boolExpr(or, exprs); }
	public static BoolExpr not(Expr... exprs) { return boolExpr(not, exprs); }
	public static Cond cond(Expr b, Expr ifExpr, Expr elseExpr) { return new Cond(b, ifExpr, elseExpr); }
	public static Cond cond(Expr b, Expr ifExpr) { return new Cond(b, ifExpr, null); }
	public static QuantifierExpr quantifierExpr(QuantifierExpr.Op op, Expr qant, Expr check) { return new QuantifierExpr(op, qant, check); }
	public static QuantifierExpr every(Expr qant, Expr check) { return quantifierExpr(QuantifierExpr.Op.every, qant, check); }
	public static Assignment assign(Expr lhs, Expr rhs) { return new Assignment(lhs, rhs); }
	public static <T> Assignment assign(Expr lhs, T o) { return new Assignment(lhs, constExpr(o)); }
	public static Filter filter(Expr... exprs) {
		return exprs.length > 1 ? filter(path(exprs)) : new Filter(exprs[0]);
	}
	public static Message message(Expr expr) { return new Message(expr); }

	//----------------- end constructors ------------------------
	
	public static boolean testIt(NodeIter nit) {

		boolean ret;
		if (nit.hasNext()) {
			Node next = nit.next();
			next.ctx.preventClearing = true;
			ret = nit.hasNext() ? true : (next.val() instanceof Boolean ? next.val() : true);
			next.ctx.preventClearing = false;
		} else {
			ret = nit == emptyTrueCut;
		}
		return ret;
	}
	
	//----------------- evaluation methods:  -------

	
	public static Stream<Node> walks(Node n, Expr... path) {return stream(walki(n, path));}
	
	public static Iterable<Node> walki(Node n, Expr... path) {
		return walki(n, new Ctx.ParamAVarEnv(), path);
	}
	
	public static Iterable<Node> walki(Node n, Ctx.ParamAVarEnv envx, Expr... path) { 
		n.ctx.initSalience(n);
		n.ctx.getVars().add(Var.of(n), "$");
		
		Iterable<Node> it = it(path(path).eval(n, envx));
		return n.ctx.salient() ? io.vavr.collection.Iterator.concat(it, Basics.action(() -> {
			n.ctx.checkSalience();
		})) : it;
	}
	
	public static Node select(Node n, Expr... path) {
		return select(n, new Ctx.ParamAVarEnv(), path);
	}

	public static Node select(Node n, Ctx.ParamAVarEnv envx, Expr... path) {		
		Node node = nil;
		for (Node n_: walki(n, envx, path)) node = n_;
		return node;
	}
	
	@FunctionalInterface
	public static interface NodeProcessing  {
		public static enum Kind {Pre, Post}
		public void process(Node x, Kind kind, int level, int orderNo, boolean isLast);
	}
	
	public static void walkr(Node n, NodeProcessing np) { walkr(n, np, 0, 0, true); }
	
	private static void walkr(Node n, NodeProcessing np, int level, int orderNo, boolean isLast) {

		np.process(n, Kind.Pre, level, orderNo, isLast);
		
		String envx = "root";
		Iterator<Node> it = path(all).eval(n, envx);
		int i = 0;
		boolean b = it.hasNext();
		while (b) {
			Node x = it.next();
			b = it.hasNext(); // lookahead !
			walkr(x, np, level + 1, i, !b);
			i++;
		}
		np.process(n, Kind.Post, level, orderNo, isLast);
	}

}
