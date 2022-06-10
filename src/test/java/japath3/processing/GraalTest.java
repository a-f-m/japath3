package japath3.processing;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static japath3.processing.Language.e_;
import static japath3.wrapper.NodeFactory.w_;

import japath3.core.Japath.Expr;
import japath3.core.Japath.NodeIter;
import japath3.core.JapathException;
import japath3.core.Node;

public class GraalTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void test() { 
		
		Context cx = Context.newBuilder().allowAllAccess(true).build();
		
		cx.eval("js", "function f(x) {\r\n"
				+ "  //x.hasNext(); y = x.next().val('b22', null); print(y === null); return x;\r\n"
				+ "  x.hasNext(); y = x.next().val('b3'); print(y === null); return x;\r\n"
				+ "}\r\n"
				+ "");
		
		try {
			
			Node n = w_(" {a: [ {b: 99, b1: {b2: 88, b3: null} }, {c: 'lala'  } ]}  ");
			Expr expr = e_("a[0].b1");
			
			NodeIter nit = expr.eval(n);

			
			
			Value func = cx.getBindings("js").getMember("f");
			if (func == null) throw new JapathException("fatal error eval pegjs");
			
			Value v = func.execute(nit);
			System.out.println(v);

		} catch (PolyglotException e) {
		e.printStackTrace();	
		}
	}

}
