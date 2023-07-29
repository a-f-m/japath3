package japath3.schema;

import static io.vavr.collection.List.empty;
import static io.vavr.control.Option.none;
import static japath3.core.Japath.__;
import static japath3.core.Japath.all;
import static japath3.core.Japath.every;
import static japath3.core.Japath.or;
import static japath3.core.Japath.path;
import static japath3.core.Japath.singleBool;
import static japath3.core.Japath.testIt;
import static japath3.core.Japath.type;
import static japath3.core.Japath.union;
import static japath3.core.Japath.BoolExpr.Op.not;
import static japath3.core.Japath.Expr.Nop;
import static japath3.processing.Language.e_;
import static japath3.processing.Language.stringify;
import static japath3.schema.Schema.MessageHandling.None;
import static japath3.schema.Schema.MessageHandling.Prefer;
import static japath3.schema.Schema.Mode.SchemaMode;
import static japath3.schema.Schema.ViolationKind.AndOp;
import static japath3.schema.Schema.ViolationKind.OrOp;
import static japath3.util.Basics.embrace;
import static japath3.util.Basics.it;
import static japath3.util.Basics.setIfAbsent;
import static org.apache.commons.lang3.StringUtils.repeat;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import io.vavr.PartialFunction;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.LinkedHashSet;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.control.Option;
import japath3.core.Ctx;
import japath3.core.Japath;
import japath3.core.Japath.BoolExpr;
import japath3.core.Japath.BoolExpr.Op;
import japath3.core.Japath.Comparison;
import japath3.core.Japath.Expr;
import japath3.core.Japath.HasType;
import japath3.core.Japath.Message;
import japath3.core.Japath.NodeIter;
import japath3.core.Japath.NodeProcessing.Kind;
import japath3.core.Japath.PathExpr;
import japath3.core.Japath.Property;
import japath3.core.Japath.QuantifierExpr;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.core.Node.PrimitiveType;
import japath3.processing.Language;
import japath3.util.Basics;
import japath3.util.Basics.Ref;

public class Schema {

	public static class SchemaBoolExpr extends BoolExpr {

		public SchemaBoolExpr(Op op, Expr[] exprs) { super(op, exprs); }

		public NodeIter eval(Node node, Object... envx) {

			Ctx ctx = node.ctx;
			boolean b = true;
			Schema schema = ctx.getSchema();
			switch (op) {
			case and:
			case not:
				// for violations in negation scope negation are not traced 
				if (op == not) envx = setIfAbsent(1, true, envx);
				
				b = true;
				int i = 0;
				String mess = null;
				for (Expr e : exprs) {
					boolean b_;
					if (i == 0 && e instanceof Message m) {
						mess = m.getMessage(node, envx);
						b_ = true;
					} else {
						b_ = testIt(e.eval(node, envx));
					}
					// if (ctx.checkValidity() && (op == not ? b : !b))
					b_ = op == not ? !b_ : b_; 
					if (!(boolean) Basics.getAt(1, envx, false) && ctx.checkValidity() && !b_) {
						schema.addViolation(AndOp, e, node, mess);
						mess = null;
					}
					b = b_ && b;
					i++;
				}
				break;
			case or:
				b = false;
				int top = schema.violations.size();
				for (Expr e : exprs)
					b = testIt(e.eval(node, envx)) || b;
				if (!(boolean) Basics.getAt(1, envx, false) && ctx.checkValidity()) {
					if (!b) {
						schema.addViolation(OrOp, this, node);
					} else {
						schema.violations = schema.violations.dropRight(schema.violations.size() - top);
					}
				}
				break;
			default:
//				throw new UnsupportedOperationException();
				return super.eval(node, envx);
			}
//			return singleBool(op == not ? !b : b, node);
			return singleBool(b, node);
		}
	}
	
	public static class SchemaQuantifierExpr extends QuantifierExpr {

		public SchemaQuantifierExpr(Op op, Expr qant, Expr check) { super(op, qant, check); }
		
		@Override public NodeIter eval(Node node, Object... envx) {
			
			if (op != Op.every) return super.eval(node, envx);
			
			Ctx ctx = node.ctx;
			NodeIter nit = exprs[0].eval(node, envx);
			boolean b = true;
			while (nit.hasNext()) {
				Node next = nit.next();
				boolean b_ = testIt(exprs[1].eval(next, envx));
				if (!(boolean) Basics.getAt(1, envx, false) && ctx.checkValidity() && !b_ && exprs[0] == all) 
					ctx.getSchema().addViolation(AndOp, this, next);
				b = b_ && b;
			}
			return singleBool(b, node);
		}
	}
	
	public static class SchemaHasType extends HasType {

		public SchemaHasType(PrimitiveType t) { super(t); }		
		
		@Override public NodeIter eval(Node node, Object... envx) {
			Ctx ctx = node.ctx;
			boolean b = node.type(t);
			if (ctx.checkValidity() && !b) 
				ctx.getSchema().addViolation(AndOp, this, node);
			return singleBool(b, node); 
		}

	}

	public static enum ViolationKind {
		AndOp, OrOp, Completeness
	}

	public static enum MessageHandling {
		None, Only, Prefer
	}

	public static enum Mode {
		SchemaMode, SelectMode
	}
	private Mode mode = SchemaMode;

	private Expr schemaExpr;
	List<Tuple2<ViolationKind, List<Object>>> violations = List.empty();
	
	Map<Object, Set<String>> propHits = LinkedHashMap.empty();

	boolean genOpt;
	boolean genSelectorRestriction;
	boolean genCompleteness = true;
//	MessageHandling genMessages = Prefer;
	MessageHandling genMessages = Prefer;
	boolean modular = true;
//	boolean modular;
	boolean jsonOutput = true;
	boolean jsonOutputErrorAsProperty;
	int defCnt;

	public void extendPropHits(Node node) {

		if (!propHits.containsKey(node.woVal())) propHits = propHits.put(node.woVal(), HashSet.empty());
	}

	public void addPropHit(Node node, String name) {
		
		if (propHits.containsKey(node.woVal())) propHits = propHits.put(node.woVal(), null, (x, y) -> {
			return x.add(name);
		});
	}
	
	public Schema genOpt(boolean b) {
		this.genOpt = b;
		return this;
	}

	public Schema genSelectorRestriction(boolean b) {
		this.genSelectorRestriction = b;
		return this;
	}

	public boolean genCompleteness() { return genCompleteness; }

	public Schema genCompleteness(boolean b) {
		this.genCompleteness = b;
		return this;
	}

	public Schema genMessages(MessageHandling h) {
		this.genMessages = h;
		return this;
	}
	
	public Schema modular(boolean b) {
		this.modular = b;
		return this;
	}
	
	public Schema jsonOutput(boolean b) {
		this.jsonOutput = b;
		return this;
	}
	
	public Schema jsonOutputErrorAsProperty(boolean b) {
		this.jsonOutputErrorAsProperty = b;
		return this;
	}

	public Schema setSchema(Expr schemaExpr) {
		violations = List.empty();
		this.schemaExpr = schemaExpr;
		// TODO here a quick 'and'-test, for now on the string
//		if (!this.schemaExpr.toString().startsWith("path(and("))
//			throw new JapathException("schema top expression has to be 'and(...)'");
		//
		return this;
	}
	
	public Schema setSchema(String schemaText) {
		setSchema(e_(schemaText, true));
		return this;
	}

	public Schema setSchema(InputStream is) {
		try {
			setSchema(IOUtils.toString(is, "utf-8"));
		} catch (IOException e) {
			throw new JapathException(e);
		}
		return this;
	}
	
	public Option<String> getValidityViolations(Node n) {
		return checkValidity(n) ? none() : Option.of(annotatedViolations(n));
	}
	
	public boolean checkValidity(Node n) {
		
		violations = List.empty();
		propHits = LinkedHashMap.empty();

		n.ctx.setCheckValidity(true, this);
		Object ok = Japath.select(n, schemaExpr).val();
		if (!(ok instanceof Boolean)) {
			throw new JapathException("constraints expression must evaluate to boolean");
		}
		// ! it is essential that 'select' precedes 'checkCompleteness' and that they are evaluated both 
		boolean b1 = (boolean) ok;
		boolean b2 = checkCompleteness(n);
		return b1 && b2;
	}
	
	public static String checkValidity(Node n, String schemaText) {
		
		return checkValidity(n, e_(schemaText, true));
	}

	public static String checkValidity(Node n, Expr schemaExpr) {
		
		Schema schema = new Schema();
		return schema.setSchema(schemaExpr).checkValidity(n) ? null : schema.annotatedViolations(n);
	}
	
//	public void reset() { violations = List.empty(); }

	private void addViolation(ViolationKind kind, Object... s) {
		violations = violations.append(Tuple.of(kind, List.of(s)));
	}

	private List<Tuple2<ViolationKind, List<Object>>> getViolations(Node n) {

		return violations.collect(new PartialFunction() {

			@Override public Object apply(Object t) { 
				return t; }

			@Override public boolean isDefinedAt(Object value) {
				return ((Node) ((Tuple2<ViolationKind, List<Object>>) value)._2.get(1)) //
						.selectorPath()
						.toString()
						.equals(n.selectorPath().toString());
			}
		});
	}
	
	public Boolean checkCompleteness(Node n) {

		Ref<Boolean> ok = Ref.of(true);
		Japath.walkr(n, (Node x, Kind kind, int level, int orderNo, boolean isLast) -> {
			if (kind == Kind.Pre) {
				if (propHits.nonEmpty() && x.isStruct()) {
					Set<String> diff = selectorDiff(x);
					if (diff.nonEmpty()) {
						ok.r = false;
						String mess = "selectors [" + diff.mkString(", ") + "] not covered by schema";
						addViolation(ViolationKind.Completeness, mess, x, mess);
					}
				}
			}
		});
		return ok.r;
	}
	
	private Set<String> selectorDiff(Node n) {
		
		HashSet<String> sels = HashSet.ofAll(it(n.childrenSelectors()));
		return sels.diff(propHits.get(n.woVal()).getOrElse(sels));
	}

	/** n has to be the same object as for {@link #checkValidity(Node)}  */
	public String annotatedViolations(Node n) {

		StringBuilder sb = new StringBuilder();
		Ref<Integer> cnt = Ref.of(0);

		Japath.walkr(n,

				(Node x, Kind kind, int level, int orderNo, boolean isLast) -> {

					if (kind == Kind.Pre) {
						sb.append(pad(level) + key(x, x.selector));
						if (x.isStruct() || x.isCheckedArray()) {
							sb.append((x.isStruct() ? "{" : "["));
						} else if (x.isCheckedLeaf()) {
							sb.append(StringUtils
									.abbreviateMiddle(embrace(x.val(), jsonOutput ? "\"" : "'") + (!isLast ? "," : ""), "...", 30));
						}
						List v = getViolations(x);
						if (v.nonEmpty()) {
							for (String s : (List<String>) violList(v, genMessages)) {
								if (jsonOutputErrorAsProperty) {
									cnt.r = cnt.r + 1;
									sb.append("\n" + pad(level) + key(x, "_error" + cnt.r));
									sb.append(embrace(s, "\""));
								} else {
									sb.append(pad(level + 1) + "// \u2190 " + s + "\n");
								}
							}
						}
						sb.append('\n');
					} else if (kind == Kind.Post) {
						if (x.isStruct() || x.isCheckedArray()) {
							sb.append(
									pad(level) + (x.isStruct() ? "}" : x.isCheckedArray() ? "]" : "") + (!isLast ? "," : "") + "\n");
						}
					}
				});

		return sb.toString();
	}
	
	private List<String> violList(List<Tuple2<ViolationKind, List<Object>>> v, MessageHandling messageHandling) {
		
		List<String> l = v.reverse().map(x -> {
			Object mess = x._2.size() == 3 ? x._2.get(2) : null;
			Object constr = x._2.get(0);
			return messageHandling != None ? mess : constr;
		}).filter(x -> {
			return x != null;
		}).map(x -> {
			return (x instanceof Expr e ? 
//					"possible correction: " +
					(e instanceof Property ? "missing: " : "") +
					stringify(e, 0) : x.toString());
		}).distinct() ;
		return messageHandling == Prefer && l.isEmpty() ? violList(v, None) : l;
	}

	private String pad(int level) { return repeat("  ", level); }

	private String key(Node x, Object keyString) {
		return x.previousNode == null || x.previousNode.isCheckedArray() ? ""
				: embrace(keyString, jsonOutput ? "\"" : "") + ": ";
	}
	
	

	public String buildConstraintText(Node n) {
		return Language.stringify(buildConstraintExpr(n), 1);
	}
	
	public Expr buildConstraintExpr(Node n) {
		
		Ref<Map<String, Expr>> defs = Ref.of(HashMap.empty());
		defCnt = 0;
		Expr e = buildConstraintExpr(n, defs);
		return modular ? Japath.path(defs.r.values().append(e).toJavaArray(Expr.class)) : e;
		
	}

	private Expr buildConstraintExpr(Node n, Ref<Map<String, Expr>> defs) {

		if (n.isStruct()) {

			Set<String> mem = HashSet.empty();
			Set<String> selectors = LinkedHashSet.empty();
			List<Expr> ands = empty();
			for (Node x : it(n.all())) {
				String sel = (String) x.selector;
				selectors = selectors.add(Language.isIdentifier(sel) ? sel : "\\Q" + sel + "\\E");
				Expr ex = buildConstraintExpr(x, defs);
				
				Expr opt =   __(sel);
				if (genOpt) opt = Japath.opt(opt);

				Expr e_;
				if (ex instanceof PathExpr pe) {
					e_ = path(Basics.prepend(opt, pe.exprs, Expr.class));
				} else {
					e_ = path(opt, ex);
				}

				if (!mem.contains(e_.toString())) {
					mem = mem.add(e_.toString());
					ands = ands.append(ex != Nop ? e_ : opt);
				} else {
//					System.out.println();
				}
			}
			if (mode == SchemaMode && genSelectorRestriction)
				ands = ands.prepend(every(all, path(Japath.sel, Japath.cmpConst(Comparison.Op.match, selectors.mkString("|")))));
			Expr[] a = ands.toJavaArray(Expr.class);
			return buildStructExpr(a, defs);

		} else if (n.isCheckedArray()) {

			Set<String> mem = HashSet.empty();
			List<Expr> ors = empty();
			for (Node x : it(n.all())) {
				Expr ex = buildConstraintExpr(x, defs);
				if (!mem.contains(ex.toString())) {
					mem = mem.add(ex.toString());
					if (ex != Nop) ors = ors.append(ex);
				}
			}
			Expr[] o = ors.toJavaArray(Expr.class);
			return buildArrayExpr(o);

			// } else if (n.isLeaf()) {

		} else {
			return mode == SchemaMode ? type(n.type()) : Nop;
		}

	}

	private Expr buildArrayExpr(Expr[] ors) {

		return //
		ors.length == 0 ? //
				Nop : (mode == SchemaMode ? //
						every(all, ors.length == 1 ? //
								ors[0] : or(ors))
						: path(all, ors.length == 1 ? //
								ors[0] : union(ors)));
	}

	private Expr buildStructExpr(Expr[] ands, Ref<Map<String, Expr>> defs) {
		
		Expr h = ands.length == 0 ? Nop //
				: ands.length == 1 ? ands[0] //
						: (mode == SchemaMode ? new SchemaBoolExpr(Op.and, ands)   : union(ands));
		if (modular) {
			String defName = "h" + defCnt++;
			defs.r = defs.r.put(defName, Japath.paramExprDef(defName, h));
			return Japath.paramExprAppl(defName);
		} else {
			return h;
		}
	}

	@Override public String toString() { return schemaExpr.toString(); }

	public Expr getSchemaExpr() { return schemaExpr; }

	public Schema setMode(Mode mode) {
		this.mode = mode;
		return this;
	}

}
