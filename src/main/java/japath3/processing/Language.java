package japath3.processing;


import static japath3.core.Japath.__;
import static japath3.core.Japath.all;
import static japath3.core.Japath.array;
import static japath3.core.Japath.assign;
import static japath3.core.Japath.bind_;
import static japath3.core.Japath.boolExpr;
import static japath3.core.Japath.cmp;
import static japath3.core.Japath.cond;
import static japath3.core.Japath.constExpr;
import static japath3.core.Japath.create;
import static japath3.core.Japath.desc;
import static japath3.core.Japath.externalCall;
import static japath3.core.Japath.filter;
import static japath3.core.Japath.message;
import static japath3.core.Japath.optional;
import static japath3.core.Japath.paramAppl;
import static japath3.core.Japath.paramExprAppl;
import static japath3.core.Japath.paramExprDef;
import static japath3.core.Japath.path;
import static japath3.core.Japath.pathAsProperty;
import static japath3.core.Japath.quantifierExpr;
//import static japath3.core.Japath.regex;
import static japath3.core.Japath.sel;
import static japath3.core.Japath.self;
import static japath3.core.Japath.struct;
import static japath3.core.Japath.subExpr;
import static japath3.core.Japath.text;
import static japath3.core.Japath.type;
import static japath3.core.Japath.union;
import static japath3.core.Japath.varAppl;
import static japath3.core.Node.nil;
import static japath3.util.Basics.embraceEsc;
import static japath3.util.Basics.it;

import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;

import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import japath3.core.Ctx;
import japath3.core.Japath.All;
import japath3.core.Japath.Array;
import japath3.core.Japath.Assignment;
import japath3.core.Japath.Bind;
import japath3.core.Japath.BoolExpr;
import japath3.core.Japath.BoolExpr.Op;
import japath3.core.Japath.Comparison;
import japath3.core.Japath.CompoundExpr;
import japath3.core.Japath.Cond;
import japath3.core.Japath.Create;
import japath3.core.Japath.Desc;
import japath3.core.Japath.Expr;
import japath3.core.Japath.ExternalCall;
import japath3.core.Japath.Filter;
import japath3.core.Japath.HasType;
import japath3.core.Japath.Idx;
import japath3.core.Japath.Optional;
import japath3.core.Japath.ParametricExprAppl;
import japath3.core.Japath.ParametricExprDef;
import japath3.core.Japath.PathAsProperty;
import japath3.core.Japath.PathExpr;
import japath3.core.Japath.Property;
import japath3.core.Japath.QuantifierExpr;
//import japath3.core.Japath.Regex;
import japath3.core.Japath.Selector;
import japath3.core.Japath.Self;
import japath3.core.Japath.Struct;
import japath3.core.Japath.SubExpr;
import japath3.core.Japath.Text;
import japath3.core.Japath.Union;
import japath3.core.Japath.VarAppl;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.core.Node.PrimitiveType;
import japath3.schema.Schema;
import japath3.schema.Schema.SchemaBoolExpr;
import japath3.schema.Schema.SchemaHasType;
import japath3.schema.Schema.SchemaQuantifierExpr;
import japath3.util.Basics;
import japath3.wrapper.NodeFactory;
import japath3.wrapper.WJsonOrg;

/**
 * For convenience we use java and vavr collections
 * @author afm
 *
 */
public class Language {
	
	public static class Env {
		io.vavr.collection.Map<String, ParametricExprDef> defs = HashMap.empty();
	}
	
	public static boolean alwaysFresh = false;
	
	private static String keywords = 
			"selector|filter|and|assert|or|xor|not|true|false|cond|imply|optional|opt|every|union|"
			+ "eq|neq|lt|gt|le|ge|call|type|self|def|def-script|new|java|j|js|match|null|error|message|property";
	
	private static PegjsEngineGraal pegjsEngine;
	
	public static int cacheMax = 500;
	private static Map<String, PathExpr> pathExprCache = Collections.synchronizedMap(Basics.createLRUMap(cacheMax));
	
	static {
		
		pegjsEngine = new PegjsEngineGraal(new InputStreamReader(Language.class.getResourceAsStream("japath-1.js")));
//		try {
//			pegjsEngine = new PegjsEngineRhino(new InputStreamReader(Language.class.getResourceAsStream("japath-1.js")));
//		} catch (IOException e) {
//			throw new JapathException(e);
//		}
	}
	
	public static PathExpr e_(String path) {
		return e_(path, false);
	}
	
	public static PathExpr e_(String path, boolean schemaProc) {
		return e_(new Env(), path, schemaProc, false);
	}
	
	public static PathExpr e_(Env env, String path, boolean schemaProc, boolean fresh) {
		
		PathExpr p = pathExprCache.get(adaptKey(schemaProc, path));
		if (p != null && !fresh && !alwaysFresh) {
			return p;
		}
		p = parse(env, path, schemaProc);
		//!!!test
//		System.out.println(">>>>>> " + stringify(p));
//		p = parse(stringify(p));
		//
		if (!fresh) pathExprCache.put(adaptKey(schemaProc, path), p);
		return p;
	}

	private static String adaptKey(boolean schemaProc, String path) { return (schemaProc ? "---schema: " : "---select") + path; }

	public static PathExpr parse(Env env, String path, boolean schemaProc) {
		
		PathExpr p;
		Tuple2<JSONObject, String> ret = getAst(path);
		
		if (ret._2 != null) {
			throw new JapathException(ret._2);
		}
		List<Expr> pe = buildExpr(env, NodeFactory.w_(ret._1, WJsonOrg.class), schemaProc);
		p = pe.head() instanceof PathExpr ? (PathExpr) pe.head() : path(pe.head());
		
		return p;
	}
	
	public static Tuple2<JSONObject, String> getAst(String path) { return pegjsEngine.getAst(path); }
	
	private static List<Expr> buildExpr(Env env, final Node ast, boolean schemaProc) {
		
		if (ast.isArray()) {
			List<Expr> exprs = List.empty();
			for (Node node2 : it(ast.all()))
				exprs = exprs.appendAll(buildExpr(env, node2, schemaProc));
			
			return exprs;
		}
		Node x;
		
		if ((x = ast.get("start").node()) != nil) return buildExpr(env, x, schemaProc);
		if ((x = ast.get("argNumber").node()) != nil) return List.of(paramAppl(x.val()));
		if ((x = ast.get("def").node()) != nil) {
			String name = x.val("name");
			if (name.matches(keywords)) throw new JapathException("def name '" + name + "' is reserved");
			ParametricExprDef ped = paramExprDef(name, buildExpr(env, x.node("expr"), schemaProc).get(0));
			if (env.defs.containsKey(name)) throw new JapathException("parametric expression '" + name + "' already defined");
			env.defs = env.defs.put(name, ped);
			return List.of(ped);
		}
		if ((x = ast.get("defScript").node()) != nil) {
			String s = x.val("s");
			s = s.replace("\\n", "\n").replace("\\r", "\r");
			
			Ctx.loadJs(new StringReader(s), "script");
			return List.of();
		}
		if ((x = ast.get("exprAppl").node()) != nil) {
			String name = x.val("name");
			Node args = x.node("args");
			return List.of(paramExprAppl(name, args == nil ? null : subExprs(env, schemaProc, x)));
		}
		if ((x = ast.get("path").node()) != nil) {
			Expr[] javaArray = subExprs(env, schemaProc, x);
			return List.of(javaArray.length == 1 ? javaArray[0] : path(javaArray));
		}
		if ((x = ast.get("step").node()) != nil) return buildExpr(env, x, schemaProc);
		if ((x = ast.get("selection").node()) != nil)  return buildExpr(env, x, schemaProc);
		if ((x = ast.get("selector").node()) != nil)  return List.of(sel);
		if ((x = ast.get("property").node()) != nil)  return List.of(__((String) x.val()));
		if ((x = ast.get("lhsProperty").node()) != nil)  return List.of(__(true, (String) x.val()));
		if ((x = ast.get("pathAsProperty").node()) != nil)
			return List.of(pathAsProperty(buildExpr(env, x, schemaProc).get(0)));
		if ((x = ast.get("subscript").node()) != nil) 
			return List.of( __((Integer) x.val(), ast.val("upper", null), ast.val("seq", false)));
		if ((x = ast.get("wild").node()) != nil)  return List.of(x.val().equals("all") ? all : desc);
		if ((x = ast.get("self").node()) != nil) return List.of(self);
		if ((x = ast.get("create").node()) != nil) return List.of(create);
		if ((x = ast.get("text").node()) != nil) return List.of(text);
		if ((x = ast.get("filter").node()) != nil) return List.of(filter(subExprs(env, schemaProc, x)));		
		if ((x = ast.get("assignment").node()) != nil) {
			return List.of( assign( //
					buildExpr(env, x.node("lhs"), schemaProc).get(0),
					buildExpr(env, x.node("rhs"), schemaProc).get(0)));
		}
		if ((x = ast.get("optional").node()) != nil) return List.of(optional(buildExpr(env, x, schemaProc).get(0)));		
		if ((x = ast.get("boolExpr").node()) != nil) {
			Expr[] subExprs = subExprs(env, schemaProc, x);
			if (schemaProc) {
				return List.of(new SchemaBoolExpr(Op.valueOf(x.val("op")), subExprs));
			} else {
				return List.of(boolExpr(Op.valueOf(x.val("op")), subExprs));
			}
		}
		if ((x = ast.get("subExpr").node()) != nil)
			return List.of(subExpr(true, subExprs(env, schemaProc, x)));
		if ((x = ast.get("struct").node()) != nil)
			return List.of(struct(subExprs(env, schemaProc, x)));
		if ((x = ast.get("quantifierExpr").node()) != nil) {
			Node q = x.node("quant");
			japath3.core.Japath.QuantifierExpr.Op op = japath3.core.Japath.QuantifierExpr.Op.valueOf(x.val("op"));
			Expr qant = q == nil ? self : buildExpr(env, q, schemaProc).get(0);
			Expr check = buildExpr(env, x.node("check"), schemaProc).get(0);
			return List
					.of(
							schemaProc ? new SchemaQuantifierExpr(op, qant, check) : 
								quantifierExpr(op, qant, check));
		}
		if ((x = ast.get("conditional").node()) != nil) {
			Node q = x.node("elseExpr");
			return List.of( cond( //
					buildExpr(env, x.node("cond"), schemaProc).get(0),
					buildExpr(env, x.node("ifExpr"), schemaProc).get(0),
					q == nil ? null : buildExpr(env, q, schemaProc).get(0)));
		}
		if ((x = ast.get("union").node()) != nil) {
			return List.of(union(subExprs(env, schemaProc, x)));
		}
		if ((x = ast.get("array").node()) != nil) {
			return List.of(array(subExprs(env, schemaProc, x)));
		}
		if ((x = ast.get("bind").node()) != nil) return List.of(bind_(x.val()) );
		if ((x = ast.get("var").node()) != nil) return List.of( varAppl(x.val()) );
		if ((x = ast.get("constant").node()) != nil) {
			Object v = x.val();
			return List.of(constExpr(isNull(v) ? Node.nullo : v));
		}
		if ((x = ast.get("compare").node()) != nil)
			return List.of(cmp(Comparison.Op.valueOf(x.val("op")), buildExpr(env, x.node("arg"), schemaProc).get(0)));
//		if ((x = ast.get("regex").node()) != nil) 
//			return List.of(regex(x.get("arg").val()));
		if ((x = ast.get("hasType").node()) != nil) {
			String val = (String) x.val();
			return List.of(schemaProc ? new SchemaHasType(PrimitiveType.valueOf(val)) : type(val));
		}
		if ((x = ast.get("funcCall").node()) != nil) {
			Node args = x.node("args");
			// so far only java
			return List.of(externalCall(x.val("kind"), x.val("ns"), x.val("func"), args == nil ? null : subExprs(env, schemaProc, x)));
		}
		if ((x = ast.get("message").node()) != nil) {
			return List.of(message(buildExpr(env, x, schemaProc).get(0)));
		}
		
		if ((x = ast.get("args").node()) != nil)  return buildExpr(env, x, schemaProc);
		
		if (ast.val().equals("empty"))  return List.empty();
		
		if (ast.val().equals("true"))  return List.of(BoolExpr.True);
		
		if (ast.val().equals("false"))  return List.of(BoolExpr.False);
		
		
//		else return List.empty();

		throw new JapathException("no ast trans for " + ast);
	}

	private static boolean isNull(Object v) { return v instanceof JSONObject && ((JSONObject) v).isEmpty(); }
	private static Expr[] subExprs(Env env, boolean schemaProc, Node x) { return buildExpr(env, x, schemaProc).toJavaArray(Expr.class); }
	
	// for flexibility reasons, e.g. other syntax forms, it is not defined 
	// as (polymorphic) 'Expr'-method with tradeoff readability
	private static String stringify0(Expr e) {
		
		if (e instanceof Bind) {
			return "$" + ((Bind) e).vname;
		} else if  (e instanceof VarAppl) {
			return "$" + ((VarAppl) e).vname;
		} else if  (e instanceof PathExpr) {
			return collect(e, ".");
		} else if  (e instanceof ParametricExprDef) {
			return "def(" + ((ParametricExprDef) e).name + ", " + stringify0(((ParametricExprDef) e).exprs[0]) + ")";
		} else if  (e instanceof ParametricExprAppl) {
			return ((ParametricExprAppl) e).name + "(" + collect(((ParametricExprAppl) e).exprs, ", ") + ")";
		} else if  (e instanceof All) {
			return "*";
		} else if  (e instanceof Desc) {
			return "**";
		} else if  (e instanceof Self) {
			return "self";
		} else if  (e instanceof Create) {
			return "new";
		} else if  (e instanceof Text) {
			return "text()";
		} else if  (e instanceof Property) {
//			return "`" + ((Property) e).name.replace("`", "\\`") + "`";
			Property p = (Property) e;
			return Language.nameForm(p.name, p.isTrueRegex);
		} else if  (e instanceof PathAsProperty) {
			return "property(" + stringify0(((PathAsProperty) e).expr) + ")";
		} else if  (e instanceof Idx) {
			String seqMark = ((Idx) e).seq ? "#" : "";
			Integer upper = ((Idx) e).upper;
			return "[" + seqMark + ((Idx) e).i + (upper != null ? ".." + ( upper != -1 ? upper : "" ) : "" ) + "]";
		} else if (e instanceof Selector) {
			return "&";
		} else if (e instanceof Union) {
			return "union(" + collect(e, ", ") + ")";
		} else if (e instanceof Array) {
			return "[" + collect(e, ", ") + "]";
		} else if (e instanceof BoolExpr) {
			Op op = ((BoolExpr) e).op;
			return op != Op.constant ? ( e instanceof Schema.SchemaBoolExpr ? (op == BoolExpr.Op.and ? "assert" : op) : op ) + "(" + collect(e, ", ") + ")" : e.toString();
		} else if (e instanceof SubExpr || e instanceof Struct) {
			return "{" + collect(e, ", ") + "}";
		} else if (e instanceof Optional) {
			return "opt(" + stringify0(((Optional) e).exprs[0]) + ")";
		} else if (e instanceof Cond) {
			Expr elsePart = ((Cond) e).exprs[2];
			return "cond(" + stringify0(((Cond) e).exprs[0])
					+ ", "
					+ stringify0(((Cond) e).exprs[1])
					+ (elsePart == Expr.Nop ? "" : ", " + stringify0(elsePart))
					+ ")";
		} else if (e instanceof QuantifierExpr) {
			QuantifierExpr qe = (QuantifierExpr) e;
			return qe.op
					+ "(" + (qe.exprs[0] instanceof Self ? "" : (stringify0(qe.exprs[0])
					+ ", ")) + stringify0(qe.exprs[1]) + ")";
		} else if (e instanceof Filter) {
			return "?(" + stringify0(((Filter) e).exprs[0]) + ")";
		} else if (e instanceof Assignment) {
			return stringify0(((Assignment) e).exprs[0]) + " : (" + stringify0(((Assignment) e).exprs[1]) + ")";
		} else if (e instanceof Comparison) {
			return ((Comparison) e).op + "(" + stringify0(((Comparison) e).exprs[0]) + ")";
		} else if (e instanceof HasType) {
			return "type(" + ((HasType) e).t + ")";
		} else if (e instanceof ExternalCall) {
			ExternalCall c = (ExternalCall) e;
			return (c.kind.equals("directive") ? "" : c.kind ) + "::" + (c.ns.equals("") ? "" : c.ns + "::") + c.func + "(" + collect(e, ", ") + ")";
		} else {
			return e.toString();
		}
	}
	
	public static String collect(Expr[] exprs, String delim) {
		
		// may be for later '?'-filter-use
		Iterator<Expr> it = Arrays.stream(exprs).iterator();
		String ret = "";
		int i = 0;
		while (it.hasNext()) {
			Expr e_ = it.next();
			ret += (i == 0 ? ""
					: (e_ instanceof Bind || e_ instanceof Filter
							|| e_ instanceof SubExpr
							|| e_ instanceof Idx ? " " : delim))
					+ stringify0(e_);
			i++;
		}
		//
		
//		String collect = e.stream().map(x -> {
//			return stringify0(x);
//		}).collect(Collectors.joining(delim));
		
//		if (!collect.equals(ret)) {
//			System.out.println();
//		}
//		return collect;
		return ret;
	}

	public static String collect(Expr e, String delim) {
		
		return  collect(((CompoundExpr) e).exprs, delim);
	}
	
	public static String stringify(Expr e, int indent) {
		return Basics.prettyNesting( //
				stringify0(e).replace(".def(", "\n.def("),
				indent,
				",",
				"\\.",
				"(type|opt|every|match|all|text|new|\\=|\\.|java|::complete|::modifiable)").replaceAll("(?m)\\(\\s*\\)", "()");
	}
	
	public static Expr clone(Expr e, boolean schemaProc) {
		return clone(new Env(), e, schemaProc);
	}
	
	public static Expr clone(Env env, Expr e, boolean schemaProc) {
		
		// have to be done due to peg engine restrictions
		boolean rootIsPath = e instanceof PathExpr;
		
		String e1 = stringify0( rootIsPath ? e : path(e));
		PathExpr e2 = e_(env, e1, schemaProc, true);
		
		return !rootIsPath ? e2.last() : e2;
		
	}
	public static String nameForm(String id, boolean isTrueRegex) {
		return isIdentifier(id) && !id.matches(keywords) ? id : embraceEsc(id, '`');
//		return isTrueRegex ? embrace(id.replace("`", "\\`"), '`')
////		return isTrueRegex ? embrace(id, '`')
////				: isIdentifier(id) && !id.matches(keywords) ? id : embrace( StringEscapeUtils.escapeJava(id), '"');
//				: isIdentifier(id) && !id.matches(keywords) ? id : embrace(id.replace("\"", "\\\""), '"');
////				: isIdentifier(id) && !id.matches(keywords) ? id : embrace(id, '"');
	}
	
	public static boolean isIdentifier(String id) { return id.matches("[a-zA-Z_][a-zA-Z0-9_]*"); }
	
	public static void main(String[] args) {

		System.out.println( "(  \r\n )".replaceAll("(?m)\\(\\s*\\)", "()")  );
	}

}
