package japath3.core;

import static japath3.core.Japath.empty;
import static japath3.core.Japath.single;
import static java.lang.Math.max;
import static java.util.Arrays.asList;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.stream.Collectors;

import org.graalvm.polyglot.Value;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.collection.Set;
import io.vavr.collection.TreeSet;
import io.vavr.control.Option;
import japath3.core.Japath.Expr;
import japath3.core.Japath.NodeIter;
import japath3.core.Japath.ParamAppl;
import japath3.core.Japath.ParametricExprDef;
import japath3.core.Japath.ParametricExprDef.FormalParam;
import japath3.core.Japath.VarAppl;
import japath3.processing.EngineGraal;
import japath3.processing.GeneralFuncs;
import japath3.processing.StandardDefs;
import japath3.processing.StringFuncs;
import japath3.processing.TimeFuncs;
import japath3.schema.Schema;
import japath3.util.Basics.Ref;

public class Ctx {
	
	public static class ParamAVarEnv {
		
		public Expr[] params;
		public Map<String, Var> vars;
		public ParamAVarEnv() {
			this(new Expr[0]);
		}
		public ParamAVarEnv(Expr[] params) {
			this.params = params;
			vars = HashMap.empty();
		}
		public Var registerVar(String name) {
			Tuple2<Var, ? extends Map<String, Var>> t = vars.computeIfAbsent(name, x -> {return new Var();} );
			vars = t._2;
			return t._1;
		}
		public Var getVar(String name) {
			Option<Var> var = vars.get(name);
			if (var.isEmpty()) {
				throw new JapathException("variable '" + name + "' not defined");
			}
			return var.get();
		}
		public static ParamAVarEnv fromEnvx(Object... envx) {
			if (envx == null || envx.length == 0 || !(envx[0] instanceof ParamAVarEnv p)) return new ParamAVarEnv();			
			return p;
		}
		public Ctx.ParamAVarEnv cloneResolvedParams(Node node, Expr[] exprs, ParametricExprDef ped, Object... envx) {
			
			params = new Expr[max(exprs.length, ped.formalParams.size())];
			try {
				int j = 0;
				ParamAVarEnv env = fromEnvx(envx);
				if (env == null) throw new JapathException("no parameter given (expr: '" + this + "')");
				for (; j < exprs.length; j++) {
						params[j] = exprs[j].deepCopy(e -> {
							
							if (e instanceof ParamAppl pa) {
								
								if (env.params.length == 0) throw new JapathException("parameter '" + this + "' not bound");
								if (pa.i >= env.params.length) throw new JapathException(
										"bad (zero-based) parameter number " + pa.i + " (parameter expressions: " + asList(env.params) + ")");
								return env.params[pa.i];
							} else if (e instanceof VarAppl va) {
								// it is ensured that it exists:
								return Japath.constNodeExpr(va.eval(node, envx).node());
							}
							return e;
						});
						
				}
				// handle defaults
				for (int k = j; k < ped.formalParams.size(); k++) {
					FormalParam formalParam = ped.formalParams.get(k);
					if (formalParam.default_() == null) {
						throw new JapathException("no parameter default for '" + formalParam + "' defined");
					}
					params[k] = formalParam.default_();
				}
			} catch (Exception e) {
				throw new JapathException(e);
			}
			return this;
		}

		@Override
		public String toString() {
			return asList(params).toString() + ", " + vars;
		}
	}

	private Schema schema;
	private boolean checkValidity;

	private Vars vars;

	boolean preventClearing;

	private boolean salient;
	public Set<String> defSelectors;
	public Set<String> undefSelectors;
	
	private static Map<String, Object> nsToEnvObj = HashMap.empty();
	// (ns, func) -> (inst, method, has-val-args)
	private static Map<Tuple2<String, String>, Tuple3<Object, Method, Boolean>> methodMap;
	
	
	private Map<String, ParametricExprDef> defs;
	
	// js
	private static EngineGraal jsEngine;
	
	static {
		loadJInst("str", new StringFuncs());
		loadJInst("time", new TimeFuncs());
		loadJInst("it", new GeneralFuncs());
		loadJInst("gen", new GeneralFuncs());
		
		initJsEngine();
	}

	public Ctx() {
		vars = new Vars();
		defSelectors = TreeSet.empty();
		undefSelectors = TreeSet.empty();
		methodMap = HashMap.empty();
		defs = HashMap.empty();
	}

	public boolean checkValidity() { return checkValidity; }

	public Ctx setCheckValidity(boolean checkValidity, Schema schema) {
		this.checkValidity = checkValidity;
		this.schema = schema;
		return this;
	}

	public void initSalience(Node n) { if (salient && defSelectors.isEmpty()) defSelectors = n.selectorNameSet(); }

	public boolean salient() { return salient; }

	public Ctx setSalient(boolean salient) {
		this.salient = salient;
		return this;
	}

	public Vars getVars() { return vars; }

	public Ctx setVars(Vars vars) {
		this.vars = vars;
		return this;
	}

	public Schema getSchema() { return schema; }

	public Ctx setSchema(Schema schema) {
		this.schema = schema;
		return this;
	}

	public Ctx clearVars() { vars.clearVars(); return this; }

	public void checkName(String name) {

		if (salient) {
			if (!defSelectors.contains(name)) undefSelectors = undefSelectors.add(name);
		}

	}

	public void checkSalience() {
		if (undefSelectors.nonEmpty()) throw new JapathException("salience: selectors " + "["
				+ undefSelectors.mkString(",")
				+ "]"
				+ " used but not found (available seletors: ["
				+ defSelectors.filter(x -> {
					return !x.trim().equals("");
				}).mkString(", ")
				+ "])");
	}
	
	public static Tuple3<Object, Method, Boolean> getTarget(String ns, String func) {
		try {
			Option<Object> inst = nsToEnvObj.get(ns);
			if (!inst.isDefined()) throw new JapathException("ns '" + ns + "' not resolvable");
			
			Class<? extends Object> clazz = inst.get().getClass();
			Method actMethod = null;
			boolean hasValArgs = false;
			Method[] methods = clazz.getMethods();
			for (Method m : methods) {
				if (m.getName().equals(func)) {
					actMethod = m;
					hasValArgs = m.getParameterTypes()[0] != Node.class;
				}
			}
			if (actMethod == null) throw new JapathException("method '" + ns + ":" + func + "' not found");
			
			return Tuple.of(inst.get(), actMethod, hasValArgs);
					
		} catch (SecurityException e) {
			throw new JapathException("cannot initialize funcCall (" + e + ")");
		}
	}
	
	@SafeVarargs
	public static void loadJInst( Tuple2<String, Object>... nss) {
		for (Tuple2<String, Object> ns : nss) {
			loadJInst(ns._1, ns._2);
		}
	}
	
	public static void loadJInst(String ns, Object o) {
		if (nsToEnvObj.containsKey(ns)) throw new JapathException("multiple definition of namespace '" + ns + "'");
		nsToEnvObj = nsToEnvObj.put(ns, o);
	}

	public static NodeIter invoke(String ns, String func, Node node, NodeIter[] nits) {

		Tuple2<String, String> nsf = Tuple.of(ns, func);
		// (inst, method, has-val-args)
		Tuple3<Object, Method, Boolean> m = methodMap.getOrElse(nsf, null);
		if (m == null) {
			m = getTarget(ns, func);
			methodMap = methodMap.put(Tuple.of(nsf, m));
		}
		try {
			try {
				if (m._3) {
					Object[] args = new Object[nits.length];
					for (int i = 0; i < args.length; i++) {
						Object val = nits[i].val(null);
						if (val == null) return empty;
						args[i] = val;
					}
					Object ret = m._2.invoke(m._1, args);
					return ret == null ? empty : Japath.singleObject(ret, node);
				} else {
					return nits.length == 0 ? (NodeIter) m._2.invoke(m._1, node) : (NodeIter) m._2.invoke(m._1, node, nits);
				}

			} catch (ClassCastException e) {
				throw new JapathException("bad result type of " + ns + ":" + func + " (" + e + ")");
			}

		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new JapathException("cannot invoke " + ns
					+ ":"
					+ func
					+ " ("
					+ (e instanceof InvocationTargetException ie? ie.getTargetException() : e)
					+ ")");
		}
	}
	
	public static void loadJs(Reader js, String name) {
		jsEngine.eval(js, name);
	}
	
	public static void initJsEngine() {
		jsEngine = new EngineGraal();
		jsEngine.eval(new InputStreamReader(Ctx.class.getResourceAsStream("std-funcs.js")), "std-js");
	}
	
	public static NodeIter invokeJs(String func, Node node, NodeIter[] nits) {

		Object[] args = buildArgs(func, node, nits);
		
		Value v = jsEngine.exec(func, args);

		Ref<Integer> i = Ref.of(0);

		if (v.hasArrayElements()) {
			
			return new NodeIter() {

				@Override public boolean hasNext() { 
					return i.r < v.getArraySize(); }

				@Override public Node next() {
					Object o = v.getArrayElement(i.r++).as(Object.class);
					check(func, i.r, o);
					return new Node.DefaultNode(o == null ? node.nullWo() : o, node.ctx);
				}

			};
		} else {
			Object o = v.as(Object.class);
			return Japath.singleObject(o == null ? node.nullWo() : o, node);
		}

	}
	
	private static Object[] buildArgs(String func, Node node, NodeIter[] nits) {
		return buildArgs(func, node, nits, false);
	}

	private static Object[] buildArgs(String func, Node node, NodeIter[] nits, boolean nonPrimitives) {
		
		Object[] args = new Object[nits.length + 1];
		args[0] = node.val();
		for (int i = 0; i < nits.length; i++) args[i + 1] = buildArg(nits[i], func, i, nonPrimitives);
		return args;
	}

	private static Object buildArg(NodeIter nodeIter, String func, int i, boolean nonPrimitives) {

		String messPrefix = "invoking js '" + func + "': " + (i + 1);

		List<Object> ol = List.empty();
		while (nodeIter.hasNext()) {
			Node n = nodeIter.next();
			if (!n.isLeaf() && !nonPrimitives)
				throw new JapathException(messPrefix + "-th argument must be a primitive value (found " + n.woVal() + ")");
			ol = ol.append(n == Node.nil || n.isNull() ? null : n.val());
		}
		
		return ol.isEmpty() ? null : (ol.size() == 1 ? ol.get(0) : ol.toJavaArray() );
		
	}

	private static void check(String func, Integer i, Object o) { 
	}

	public NodeIter invokeDirective(String ns, String func, Node node, NodeIter[] nits) {

		switch (ns) {
		case "": // e.g. d:complete

			if (this.schema == null) {
				// throw new JapathException("not in schema mode");
				// TODO Robustness
				this.schema = new Schema();
			}
			switch (func) {
			case "complete":
				schema.extendPropHits(node);
				return single(node);
			// deferred
			case "modifiable":
				if (node != node.ctx.getVars().getVar("$").node())
					throw new JapathException("only the root node is modifiable as a whole");
				return single(node.setConstruct(true));
			case "log":
				System.out.println(">>>log: " + asList(buildArgs(func, node, nits, true)).toString());
				return single(node);
			case "nop":
				return single(node);
			case "error":
				String mess = asList(nits).stream().map(x -> {
					return x.val().toString();
				}).collect(Collectors.joining(" "));
				
				throw new JapathException("error at node " + node + ": " + mess);
			}
			break;
		}
		throw new JapathException("directive '" + ns + ":" + func + "' not found");
	}

	public void declare(String name, ParametricExprDef ped) {
		
		if (StandardDefs.globalDefs.containsKey(name)) throw new JapathException("global parametric expression '" + name + "' already defined");
		if (defs.containsKey(name)) throw new JapathException("parametric expression '" + name + "' already defined");
		defs = defs.put(name, ped);
	}
	
	public Ctx clearDefs() { defs = HashMap.empty(); return this; }
	
	public ParametricExprDef getPed(String name) {
		ParametricExprDef gped = StandardDefs.globalDefs.get(name).getOrNull();
		if (gped != null) {
			return gped;
		}
		
		Option<ParametricExprDef> d = defs.get(name);
		if (!d.isDefined()) throw new JapathException("parametric expression '" + name + "' not defined");
		return d.get();
	}

	public Ctx setDefs(Map<String, ParametricExprDef> defs) {
		this.defs = defs;
		return this;
	}
	
}
