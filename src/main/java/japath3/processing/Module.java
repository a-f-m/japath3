package japath3.processing;

import static japath3.processing.Language.e_;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import japath3.core.Ctx;
import japath3.core.Japath;
import japath3.core.Japath.Expr;
import japath3.core.Japath.ParametricExprDef;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.core.Vars;
import japath3.processing.Language.Env;
import japath3.schema.Schema;
import japath3.schema.Schema.MessageHandling;

public class Module {
	
	private String name;
	private Env env;
	private boolean isSchemaModule;
	// if a schema module:
//	private boolean genMessages;// = true;
	
	public Module(String name, String pathExprStr, boolean isSchemaModule) {
		init(name, pathExprStr, isSchemaModule);
	}

	public Module(String name, String pathExprStr) {

		this(name, pathExprStr, false);
	}
	
	public Module(String name, InputStream is) {
		this(name, is, false);
	}

	public Module(String name, InputStream is, boolean isSchemaModule) {
		try {
			init(name, IOUtils.toString(is, "utf-8"), isSchemaModule);
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}
	
	private Module init(String name, String pathExprStr, boolean isSchemaModule) {

		this.name = name;
		this.isSchemaModule = isSchemaModule;

		env = new Env();
		// env is populated aside;
		e_(env, pathExprStr, isSchemaModule, true);
		return this;
	}
	
	public Module importModule(Module... modules) {
		
		for (Module importModule : modules) {
			for (Tuple2<String, ParametricExprDef> entry : importModule.env.defs) {
				
				if (env.defs.containsKey(entry._1)) {
					throw new JapathException(
							"import of parametric expression '" + entry._1 + "' from module '" + importModule.name
									+ "' failed'. "
									+ "Already defined in module '" + name + "'");
				}
				env.defs = env.defs.put(entry._1, entry._2);
			}
		}
		return this;
	}
	
	public Expr getExpr(String exprName) {
		return getParametricExprDef(exprName).exprs[0];
	}
	
	public ParametricExprDef getParametricExprDef(String exprName) {
		Option<ParametricExprDef> ped = env.defs.get(exprName);
		if (ped.isEmpty()) throw new JapathException("expression '" + exprName + "' not defined in module '" + name + "'");
		return ped.get();
	}

	@SuppressWarnings("unused")
	private Expr getExprCopy(String exprName, Expr[] params) {
		
		Expr expr = getExpr(exprName);
		try {
			return expr.deepCopy(Expr.identity());
		} catch (CloneNotSupportedException e) {
			throw new JapathException(e);
		}
	}
	
	public Node trans(Node n, String exprName, Object... paramObjs) {
		return trans(n, exprName, null, paramObjs);
	}
	
	public Node trans(Node n, String exprName, Vars vars, Object... paramObjs) {
		
		prepare(n, vars);		
		Expr[] params = List.of(paramObjs).map(x -> {
			boolean primitive = x instanceof Number || x instanceof Boolean || x instanceof String;
			if (!(x instanceof Expr || x instanceof Node || x instanceof List || primitive)) {
				throw new JapathException(x + " must be primitive type or an expression or (list of) node");
			}
			return primitive ? Japath.constExpr(x)
					: x instanceof Node n_ ? Japath.constNodeExpr(n_)
							: x instanceof List l ? Japath.constNodeListExpr(l) : (Expr) x;
		}).toJavaArray(Expr.class);
		
		try {
			ParametricExprDef ped = getParametricExprDef(exprName);
			return Japath.select(n, new Ctx.ParamAVarEnv(params).cloneResolvedParams(n, params, ped), ped.exprs[0]);
		} catch (JapathException e) {
			throw new JapathException("error at module '" + name + "', evaluation tree beneath def '" + exprName + "': " + e.getMessage());
		}
	}
	
	public static record ValidationResult(String violations, String schema) {}
	
	public Option<ValidationResult> checkSchema(Node jo, String schemaName) {
		return checkSchema(jo, schemaName, null);
	}
	
	/**
	 * returns (violations, schema) 
	 */
	public Option<ValidationResult> checkSchema(Node n, String schemaName, Vars vars) {
		
		if (!isSchemaModule) throw new JapathException("'" + name + "' is not a schema module");
		prepare(n, vars);
		
//		Schema schema = new Schema().setSchema(getExpr(schemaName)).genMessages(MessageHandling.None);
		Schema schema = new Schema().setSchema(getExpr(schemaName)).genMessages(MessageHandling.Prefer);
//		Schema schema = new Schema().setSchema(getExpr(schemaName)).genMessages(MessageHandling.Only);
		Option<String> validityViolations = schema.getValidityViolations(n);

		return validityViolations.isDefined()
				? Option.of( new ValidationResult(validityViolations.get(), Language.stringify(schema.getSchemaExpr(), 2)))
				: Option.none();				
	}

	private void prepare(Node n, Vars vars) {
		n.ctx.setDefs(env.defs);
		if (vars != null) n.ctx.setVars(vars);
	}

}
