package japath3.processing;

import static japath3.processing.Language.e_;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

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

public class Module {
	
	private String name;
	private Env env;
	private boolean isSchemaModule;
	// if a schema module:
	private boolean genMessages;// = true;
	
	public Module(String name, String pathExprStr, boolean isSchemaModule) {
		init(name, pathExprStr, isSchemaModule);
	}

	public Module(String name, String pathExprStr) {

		this(name, pathExprStr, false);
	}
	
	public Module(String name, InputStream pathExprStr) {
		this(name, pathExprStr, false);
	}

	public Module(String name, InputStream pathExprStr, boolean isSchemaModule) {
		try {
			init(name, IOUtils.toString(pathExprStr, "utf-8"), isSchemaModule);
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
		Option<ParametricExprDef> ped = env.defs.get(exprName);
		if (ped.isEmpty()) throw new JapathException("expression '" + exprName + "' not defined in module '" + name + "'");
		return ped.get().exprs[0];
	}

	@SuppressWarnings("unused")
	private Expr getExprCopy(String exprName, Expr[] params) {
		
		Expr expr = getExpr(exprName);
		try {
			return expr.deepCopy(params, new HashMap<String, Japath.Bind>());
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
			if (!(x instanceof Expr || primitive)) {
				throw new JapathException(x + " must be of primitive type");
			}
			return primitive ? Japath.constExpr(x) : (Expr) x;
		}).toJavaArray(Expr.class);
		
		return Japath.select(n, new Ctx.ParamAVarEnv(params), getExpr(exprName));
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
		
		Schema schema = new Schema().setSchema(getExpr(schemaName)).genMessages(genMessages);
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
