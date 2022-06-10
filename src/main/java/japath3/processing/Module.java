package japath3.processing;

import static japath3.processing.Language.e_;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.apache.commons.io.IOUtils;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;
import io.vavr.control.Option;
import japath3.core.Japath;
import japath3.core.Japath.Expr;
import japath3.core.Japath.ParametricExprDef;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.processing.Language.Env;
import japath3.schema.Schema;

public class Module {
	
	private String name;
	private Env env;
	private boolean isSchemaModule;
	
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
		e_(env, pathExprStr, isSchemaModule, true);
		return this;
	}
	
	public Expr getExpr(String exprName) {
		return getExpr(exprName, new Expr[0]);
	}

	public Expr getExpr(String exprName, Expr[] params) {
		
		Option<ParametricExprDef> ped = env.defs.get(exprName);
		if (ped.isEmpty()) throw new JapathException("expression '" + exprName + "' not defined in module '" + name + "'");
		try {
			return ped.get().exprs[0].deepCopy(params, new HashMap<String, Japath.Bind>());
		} catch (CloneNotSupportedException e) {
			throw new JapathException(e);
		}
	}
	
	public Node trans(Node n, String exprName, Object... paramObjs) {
		
		n.ctx.setDefs(env.defs);
		
		return Japath.select(n, getExpr(exprName, paramObjs.length > 0 ? List.of(paramObjs).map(x -> {
			boolean primitive = x instanceof Number || x instanceof Boolean || x instanceof String;
			if (!(x instanceof Expr || primitive)) {
				throw new JapathException(x + " must be of primitive type");
			}
			return primitive ? Japath.constExpr(x) : (Expr) x;
		}).toJavaArray(Expr.class) : new Expr[0]));
	}
	
	
	public Option<Tuple2<String, String>> checkSchema(Node jo, String schemaName) {
		
		if (!isSchemaModule) throw new JapathException("'" + name + "' is not a schema module");

		Schema schema = new Schema().setSchema(getExpr(schemaName));
		Option<String> validityViolations = schema.getValidityViolations(jo);

		return validityViolations.isDefined()
				? Option.of(Tuple.of(validityViolations.get(), Language.stringify(schema.getSchemaExpr(), 2)))
				: Option.none();				
	}

}
