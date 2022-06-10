package japath3.core;

import static japath3.core.Japath.__;
import static japath3.core.Japath.assign;
import static japath3.core.Japath.bind_;
import static japath3.core.Japath.c_;
import static japath3.core.Japath.create;
import static japath3.core.Japath.empty;
import static japath3.core.Japath.p_;
import static japath3.core.Japath.select;
import static japath3.core.Japath.single;
import static japath3.core.Japath.subExpr;
import static japath3.core.Japath.varAppl;
import static japath3.core.JapathTest.assertIt;
import static japath3.processing.Language.e_;
import static japath3.wrapper.NodeFactory.w_;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.FileReader;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.graalvm.polyglot.Value;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vavr.Tuple;
import japath3.core.Japath.Expr;
import japath3.core.Japath.NodeIter;
import japath3.core.Japath.PathExpr;
import japath3.core.Japath.Selection;
import japath3.processing.EngineGraal;
import japath3.processing.Language;
import japath3.util.Basics;
import japath3.util.Testing;

public class JapathEnhTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void testNew() { 
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		assertIt(n, "[lala | (x, ^``->undef)]", "new$x.$.a.c", true);
		
		n.ctx.clearVars();
		try {
			assertIt(n, "[lala | (x, ^``->undef)]", 
					"   def(do, ?(#0)). "
							+ " do(new $x). a.c", true, true);
			fail();
		} catch (JapathException e) {
			System.out.println(e);
		}

		
		n.ctx.clearVars().clearDefs();
		assertIt(n, "[lala | (x, ^``->undef)]", 
				"   def(do, ?(#0)). "
						+ " do(new $x: true). a.c", true, true);
	}
	
	@Test public void testNull() {
		
		Node n = w_(" {a: {b: null, c: [null]} }  ");
		
		try {
			assertIt(n, "", "a.b.type('hi')");
			fail();
		} catch (JapathException e) {
			// ok
		}
		
		assertIt(n, "[true]", "a.b.eq(null)");
		
		assertIt(n, "[false]", "a.b.neq(null)");
		
		assertIt(n, "[false]", "a.c[0].neq(null)");
		
		try {
			assertIt(n, "[false]", "a.c[0].gt(null)");
			fail();
		} catch (JapathException e) {
			// ok
		}
		
		assertIt(n, "[]", "a.b.x");
		
		assertIt(n, "[{\"a\":null}]", "{a: null}");
	}

	
	@Test 
//	@Ignore // TODO deferred
	public void testModify() { 
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		String exp = "[{\"a\":{\"b\":99,\"c\":\"lala\"}} | ]";
		
		assertIt(n, exp, "::modifiable.a{b:99}.$", true);
		assertIt(n, exp, "::modifiable{ cond(a, a).b:99}.$", true); // TODO

		try {
			assertIt(n, "", "_{a.::modifiable}.a{b:99}.$", true);
			fail();
		} catch (Exception e) {
			System.out.println(e);
		}
		
		n = w_(" {a: {b: false, c: [2,3,4]} }  ").setConstruct(true);
		assertIt(n, "[{\"a\":{\"b\":false,\"c\":[2,99,4]}} | ]", "a{c[1]:(99)}.$", true);
		
		n = w_("[2,3,4]");
		
		assertIt(n, "[[2,99,4] | ]", "::modifiable._{_[1]:(99)}.$", true);
		
		n = w_(" {a: {b: {b1: 88}, c: 'lala'} }  ");
		
		n.ctx.setSalient(true);
		assertIt(n, "[{\"a\":{\"b\":{\"b1\":88},\"c\":{\"b1\":88}}} | ]", "::modifiable.a{c:(b)}.$", true);
		
		n = w_(" {a: {b: {b1: 88}, c: 'lala'} }  ");
		
		try {
			// recursion
			assertIt(n, "[{\"a\":{\"b\":{\"b1\":88},\"c\":{\"b\":{\"b1\":88},\"c\":\"lala\"}}} | ]", "::modifiable.a{c:$.a}.$", true);
			fail();
		} catch (Exception e) {
		}
		
		n = w_(" {a: [null, null] }  ");
		
		assertIt(n, "[{\"a\":[99,99]} | ]", "::modifiable._{a.* : 99}.$", true);

		n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		assertIt(n, "[{\"b\":99,\"c\":\"lala\",\"d\":88} | ]", "::modifiable.a{b:99, d:88}", true);
	}
	
	@Test 
//	@Ignore // TODO deferred
	public void testModifyRaw() {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ").setConstruct(false);
		
		// "a{b=(99)}.$"
		JapathTest.assertIt(n, "[{\"a\":{\"b\":99,\"c\":\"lala\"}} | ]", 
				
				p_(Japath.externalCall("directive", "", "modifiable", null),
				__("a"), subExpr(assign(p_(__("b")), c_(99))), varAppl("$")),
				
				true, false);
	}
	
	@Test public void testConstruct() {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		assertIt(n, "[{\"a\":{\"b\":99}} | (x, ^``->{\"a\":{\"b\":99}})]", "new $x :{a.b:99}", true);
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":[null,{\"b\":99}]} | (x, ^``->{\"a\":[null,{\"b\":99}]})]", 
				
				"new $x{a[1].b:(99)}. $x", true);
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":{\"b\":[null,99]}} | (x, ^``->{\"a\":{\"b\":[null,99]}})]", 
				
				"new $x{a.b[1]:(99)}", true);
		
		n.ctx.clearVars();
		assertIt(n, "[[null,99] | (x, ^``->[null,99])]", 
				
				"new $x{_[1]:(99)}. $x", true);
		
		n.ctx.clearVars();
		String expr1 = "new { a : new{b : 99, b1 : {\"c1\" : 88}}, c : union(1, 2, new{\"d\" : \"lala\"}) }";
		Expr expr = e_(expr1);
		System.out.println(Language.stringify(expr, 1));
		assertIt(n, "[{\"a\":{\"b\":99,\"b1\":{\"c1\":88}},\"c\":[1,2,{\"d\":\"lala\"}]}]", 
				
				expr, false, false);
	}
	
	@Test public void testConstruct1() {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		n.ctx.clearVars();
		
		assertIt(n, "[{\"a\":{\"d\":[]}}]", 
				
				"new $x:{"
						+ "  a: {"
						+ "   d : [ ] "
						+ "}"
						+ "}");

		n.ctx.clearVars();

		assertIt(n, "[{\"a\":{\"b\":{\"c\":\"lala\"},\"d\":99}}]", 
				
				"new $x:{"
				+ "  a: {"
				+ "   b: {c:'lala'},"
				+ "   d : 99"
				+ "}"
				+ "}");
		
		n.ctx.clearVars();

		assertIt(n, "[{\"a\":{\"b\":{\"c\":\"lala\"},\"d\":[null,88,77]}}]", 
				
				"new $x:{"
				+ "  a: {"
				+ "   b: {c:'lala'},"
				+ "   d[1] : 88,"
				+ "   d[2] : 77"
				+ "}"
				+ "}");
		
		n.ctx.clearVars();
		
		assertIt(n, "[{\"a\":[1,2,3]}]", "new $x{a:union(1, 2, 3)}");
		
		n = w_(" {a: {b: [7, 8, 9], c: 'lala'} }  ");
		
		assertIt(n, "[{\"a\":[7,8,9]}]", "new $x{a:$.a.b}");
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":[7,8,9]} | (x, ^``->{\"a\":[7,8,9]})]", "new $x:{a:a.b}", true);
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":[7,8,9]}]", "new $x{a:$.a.b.*}");
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":{\"b\":[7,8,9],\"c\":\"lala\"}}]", "new $x:{a:cond(true, a)}");
		
		n.ctx.clearVars();
		assertIt(n, "[{\"b\":88}]", "new $x{cond(true, b):88}");
		
		n.ctx.clearVars();
		try {
			assertIt(n, "", "new $x{cond(a.eq(1), b):88}");
			fail();
		} catch (JapathException e) {
			assertEquals("operation on undef node '`a`->undef' (assign value first)",
					e.getMessage());
		}

		n.ctx.clearVars();
		assertIt(n, "[{\"a\":1,\"b\":88}]", "new $x{a:1, cond(a.eq(1), b):88}"); // TODO really OK?
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":1,\"c\":88}]", "new $x{a:1, cond(a.eq(2), b, c):88}"); 

		n.ctx.clearVars();
		try {
			assertIt(n, "", "new $x{`.*`:88}");
			fail();
		} catch (JapathException e) {
			assertEquals("operation on undef node '``->undef' (assign value first)",
					e.getMessage());
		}
		
		
		n.ctx.clearVars();
		assertIt(n, "[{\"a\":[99]}]", "new $x:{a:[99]}");
		
		n.ctx.clearVars();
		try {
			assertIt(n, "", "new $x:{cond(a, a):99}");
			fail();
		} catch (JapathException e) {
			assertEquals("operation on undef node '`a`->undef' (assign value first)",
					e.getMessage());
		}
		
		n.ctx.clearVars();
		try {
			// rhs is undef
			assertIt(n, "[{}]", "new $x:{a:b}");
//			fail();
		} catch (JapathException e) {
			// ok
		}
	}
	
	@Test public void testConstruct2() {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
//		try { TODO check exists
			assertIt(n, "[{\"a\":2}]", "new : {a:1, a:2}");
//			fail();
//		} catch (JapathException e) {
//			assertEquals("'a' already set: ``->{\"a\": 1}",
//					e.getMessage());
//		}
			
		try {
			// rhs is undef
			assertIt(n, "[{}]", "new: {a:b}");
//			fail();
		} catch (JapathException e) {
			// ok
		}

		assertIt(n, "[lala]", "{a:'lala'}.a");
		
		assertIt(n, "[{}]", "{}");
		
		assertIt(n, "[{}]", "new: {}");
		
		assertIt(n, "[[]]", "[]");
		
		assertIt(n, "[[1,2]]", "[1,2]");
		
		assertIt(n, "[1]", "[1,2][0]");
		
		assertIt(n, "[1, 2]", "union(1,2)[#0]");
		
		assertIt(n, "[{\"b\":false,\"c\":\"lala\"}]", "{a:[a, 99]}.a[0]");
		
		try {
			assertIt(n, "", "new {a:b}");
			fail();
		} catch (JapathException e) {
			System.out.println(e);
		}
		
	}
	
	@Test public void testConstructJsonSynt() {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		n.ctx.clearVars();
		
		assertIt(n, "[{\"a\":{\"b\":{\"c\":\"lala\"},\"d\":99,\"e\":[1,2,\"la\",[{\"e1\":9},88]]}}]", 
				
				"{"
						+ "  \"a\" : {"
						+ "   \"b\" : {c:'lala'},"
						+ "   \"d\" : 99,"
						+ "   e: [1, 2, \"la\", [{e1:9}, 88] ]"
						+ "  }"
						+ "}");
		
		n.ctx.clearVars();
		
		assertIt(n, "[{\"a\":{\"b\":{\"c\":\"lala\"},\"d\":[null,88,77]}}]", 
				
				"{"
						+ "  \"a\" : {"
						+ "   `b` :  { c:'lala'},"
						+ "   d[1] : 88,"
						+ "   d[2] : 77"
						+ "  }"
						+ "}");
		
		n.ctx.clearVars();
		
		assertIt(n, "[17]", 
				
				"{ \"pers\": {\r\n"
				+ "    \"name\": \"Miller\",\r\n"
				+ "    \"age\": 17\r\n"
				+ "}}.pers.age");
		
		
		n.ctx.clearVars();
		try {
			assertIt(n, "[17]", 
					
					"{ \"pers\": {\r\n"
							+ "    \"name\": \"Miller\",\r\n"
							+ "    \"age\": 17\r\n"
							+ "}}.x");
			fail();
		} catch (JapathException e) {
			System.out.println(e);
		}

		
	}
	
	@Test public void doConstructMultiLineStr() {
		
		Node n = w_(" {a: {b: false, c: 'lala'} }  ");
		
		String s = "{\n"
				+ "    type: 'lala',\n"
				+ "    x: 2.2,\n"
				+ "    y: true,\n"
				+ "    apath: \"\"\"\n"
				+ "        sjdfdf\n"
				+ "        sajdjdj \n"
				+ "        397375§%%\n"
				+ "        \"\"\"\n"
				+ "}";
		
		Node select = Japath.select(n, e_(s));
		System.out.println(
				((JSONObject) select.wo).toString(3)
				);
		System.out.println(
				((JSONObject) select.wo).getString("apath").replace("\\n", "\n")
				);
	}
	
	@Test public void testConstructMix() {
		
		Node n = w_("{\r\n"
				+ "    \"text\": \"lolo lili lolo lili \",\r\n"
				+ "    \"name\": {\r\n"
				+ "        \"first\": \"john\",\r\n"
				+ "        \"last\": \"miller\"\r\n"
				+ "    },\r\n"
				+ "    \"age\": 99\r\n"
				+ "}");
		
		PathExpr e = e_("new $x{text:$.text, name:$.name, age:$.age}");
		
		System.out.println(e);
		
		@SuppressWarnings("unused")
		Var x = Var.of();
		PathExpr path = p_(create,
				bind_("x"),
				subExpr(assign(p_(__("text")), p_(varAppl("$"), __("text"))),
						assign(p_(__("name")), 
								select(n, e_("name.last")).val() + ", " + select(n, e_("name.first")).val() 
								),
						assign(p_(__("age")), p_(varAppl("$"), __("age")))));
		
		select(n, path);
//		select(n, e);
//		System.out.println(n.ctx.getVars());
//		assertEquals( cr("(x, ^``->{\"name\":\"miller, john\",\"text\":\"lolo lili lolo lili \",\"age\":99})") , n.ctx.getVars().toString());
		
		
	}
	
	@Test public void testJavaCall() {
		
		final class Xxx {
			@SuppressWarnings("unused") // here in scope
			public NodeIter f(Node ctxNode, NodeIter[] nits) {
				
				return nits[0].hasNext() ? single(nits[0].next()) : empty;
				
			}
			@SuppressWarnings("unused") // here in scope
			public NodeIter g(Node ctxNode) {
				
				return single(ctxNode);
				
			}
			@SuppressWarnings("unused")
			public String conc(String s1, String s2) {
				return s1 + s2;
			}
		}
		
		Node n = w_(" {a: {b: false, c: 'lala', d: 'lolo', e: 'a;b', f: '18.03.2021', f1: '3/18/2021', g:'aa|bb', h:'2017'} }  ");		
		
		Ctx.loadJInst("m", new Xxx());
		assertIt(n, "[lala]", "j::m::f(a.c)");
		assertIt(n, "[false]", "j::m::f(a.b)");

		assertIt(n, "[lala]", "a.c.j::m::g()"); 

		assertIt(n, "[lalalolo]", "j::m::conc(a.c, a.d)");

		// predef std func
		assertIt(n, "[lalalolo]", "j::str::conc(a.c, a.d)");

		assertIt(n, "[]", "j::m::conc(a.c, a.x)");
		
		
		Ctx.loadJInst(Tuple.of("x", (Expr) x -> {
			return single(x);
		}));
		
		assertIt(n, "[lala]", "a.c.j::x::eval()");
		
		try {
			assertIt(n, "[lala]", "a.type('hi')");
			fail();
		} catch (JapathException e) {
			System.out.println(e);
		}
		
		try {
			assertIt(n, "[lala]", "a.type()");
			fail();
		} catch (JapathException e) {
			System.out.println(e);
		}
		
		assertIt(n, "[a, b]", "a.e.j::str::split(';')");

		assertIt(n, "[aa, bb]", "a.g.j::str::split('\\|')");

		assertIt(n, "[1917]", "a.h.j::str::replace('20', '19')");

		assertIt(n, "[2021-03-18T00:00]", "j::time::germanToIsoDate(a.f)");
		
		assertIt(n, "[2021-03-18T00:00]", "j::time::usToIsoDate(a.f1)");
		
		assertIt(n, "[{\"b\":99}]", "{ a: '{ b: 99 }' }.a.j::str::stringToJson()");
		
		n = w_(" {a: [1,2,2,3] }  ");		
		
		assertIt(n, "[1, 2, 3]", "j::it::distinct(a.*)");

		n = w_(" {a: {a1:false}, b: 'lala', c: 99, d: null}  ");
		
		assertIt(n, "[{\"b\":\"lala\"}]", "j::it::project('b')");
		
		assertIt(n, "[{\"a\":{\"a1\":false},\"b\":\"lala\"}]", "j::gen::project(['a', 'b'].*)");
		
		
	}
	
	@Test public void testRawScript() throws Exception {

		FileReader r = new FileReader("src/test/resources/japath3/core/script-1.js");
		EngineGraal eng = new EngineGraal().eval(r, "test");

		Node n = w_(" {a: false, b: 'lala', c: 99, d: null}  ");		
		
		Value v = eng.exec("f", n.val(), n.get("a").val(), n.get("b").val(), n.get("c").val(), n.get("d").val());
		
		Value a3 = v.getArrayElement(3);
		assertEquals("null", a3.toString());
		v.removeArrayElement(3);
		assertEquals("(4)[true, \"lalax\", 100, null]", v.toString());

//		v = eng.exec("parseFloat", n.val(), n.get("c").val());
//		for (int i = 0; i < 10000; i++) {
//			v = eng.exec("eval", " print(1)");
//		}
//		v = eng.exec("print", 1);
		
//		assertEquals("(5)[true, \"lalax\", 100, JavaObject[org.json.JSONObject$Null], null]", v.toString());
		
		r = new FileReader("src/test/resources/japath3/core/script-1.js");
		Ctx.loadJs(r, "test");
	
		NodeIter nit = Ctx.invokeJs("f", n, new NodeIter[] {n.get("a"), n.get("b"), n.get("c"), n.get("d")});
		assertEquals("[true, lalax, 100, null, null]", Basics.stream((Iterable) nit).collect(Collectors.toList()).toString());
	}
	
	@Test public void testJsCall() throws Exception {
		
		Node n = w_(" {a: false, b: 'lala', c: 99, d: null}  ");
		
		FileReader r = new FileReader("src/test/resources/japath3/core/script-1.js");
		Ctx.initJsEngine();
		Ctx.loadJs(r, "test1");
		
		assertIt(n, "[true, lalax, 100, null, null]", "js::f(a, b, c, d)");

		assertIt(n, "[true, nullx, 100, null, null]", "js::f(a, bb, c, d)");
		
		try {
			assertIt(n, "", "js::f(_, b, c, d)");
			fail();
		} catch (JapathException e) {
			assertEquals("invoking js 'f': 1-th argument must be a primitive value (found {\"a\":false,\"b\":\"lala\",\"c\":99,\"d\":null})", e.getMessage());
		}

		try {
			assertIt(n, "", "js::ff()");
			fail();
		} catch (JapathException e) {
			assertEquals("japath3.core.JapathException: js func 'ff' does not exists", e.getMessage());
		}
		
		Ctx.initJsEngine();
		assertIt(n, "[true, lalax, 100, null, null]", 
				
				""
				+ "def-script("
				+ "\"\"\""
				+ "function f(x, a, b, c, d) {\r\n"
				+ "	\r\n"
				+ "	print(x, a, b, c, d)\r\n"
				+ "	return [!a, b + 'x', c + 1, d, null]\r\n"
				+ "}"
				+ "\"\"\")."
				+ " js::f(a, b, c, d)");
		
		Ctx.initJsEngine();
		assertIt(n, "[-1]", 
				
				""
						+ "def-script("
						+ "\"\"\""
						+ "function f(x) {\r\n"
						+ "	\r\n"
						+ "	print(x)\r\n"
						+ "	return x === 'lala' ? -1 : x + 1\r\n"
						+ "}"
						+ "\"\"\")."
						+ " b.js::f()");
		
		// Overwrite f
		try {
			assertIt(n,
					"[99]",

					"" + "def-script("
							+ "\"\"\""
							+ "function f(x, a, b, c, d) {\r\n"
							+ "	\r\n"
							+ "	print(x, a, b, c, d)\r\n"
							+ "	return [!a, b + 'x', c + 1, d, null]\r\n"
							+ "}"
							+ "\"\"\")."
							+ "def-script("
							+ "\"\"\""
							+ "function f(x, a, b, c, d) {return 99}"
							+ "\"\"\")."
							+ " js::f(a, b, c, d)");
			fail();
		} catch (JapathException e) {
			assertEquals("japath3.core.JapathException: javascript func 'f' already defined", e.getMessage());
		}

		Ctx.initJsEngine();
		String script = ""
				+ "def-script("
				+ "\"\"\""
				+ "load('src/test/resources/japath3/core/script-1.js')\r\n"
				+ "function g(x, a, b, c, d) {"
				+ "return f(x, a, b, c, d)"
				+ "}"
				+ "\"\"\")."
				+ " js::g(a, b, c, d)";
		System.out.println(script);
		assertIt(n, "[true, lalax, 100, null, null]", 				
				script);

		// 'npm install -g browserify'; 'browserify --standalone xxx index.js -o bundle.js'
		Ctx.initJsEngine();
		script = ""
				+ "def-script("
				+ "\"\"\""
				+ "load('src/test/resources/japath3/core/bundle.js')\r\n"
//				+ "load('https://github.com/a-f-m/japath3/raw/main/src/test/resources/japath3/core/bundle.js')\r\n"
				+ "function g() {"
				+ "const encode = bundle.Buffer.from('lalilu').toString('base64')\r\n"
				+ "console.log(encode);"
				+ "return encode"
				+ "}"
				+ "\"\"\")."
				+ " js::g()";
		System.out.println(script);
		assertIt(n, "[bGFsaWx1]", 				
				script);
	}
	
	@Test public void testJsStdFuncs() throws Exception {
		
		Node n = w_(" {a: false, b: 'lala', c: 99, d: null}  ");
		
		assertIt(n, "[11;22]", "js::fjoin(';', '11', '22')");
		
		n = w_(" [ {a: ['1', '2']}, {b: ['3', '4']} ]");
		
		assertIt(n, "[1+2, 3+4]", "*.*.js::fjoin('+', *)");
		
		assertIt(n, "[1+2*3+4]", "js::fjoin('*', *.*.js::fjoin('+', *))");
		
		// non-primitive
		try {
			assertIt(n, "[1+2, 3+4]", "*.js::join('+', *)");
			fail();
		} catch (JapathException e) {
			assertEquals("invoking js 'join': 2-th argument must be a primitive value (found [\"1\",\"2\"])", e.getMessage());
		}
	}
	
	@Test public void testLhsRhs() {
		
		String e = "_{x:y,z,new $x:{"
				+ "  a: {"
				+ "   b: {c:'lala'},"
				+ "   d : 99"
				+ "}"
				+ "}}";
		
		PathExpr expr = e_(e);
		
		StringBuilder sb = new StringBuilder();
		expr.visit(
				(x, pre) -> {
					if (x instanceof Selection) 
						sb.append(pre + ">>> " + ((Selection) x).scope + " >>> " + x + "\r\n");
				}
				);
		assertEquals("true>>> lhs >>> __(\"x\")\r\n"
				+ "true>>> rhs >>> __(\"y\")\r\n"
				+ "true>>> none >>> __(\"z\")\r\n"
				+ "true>>> lhs >>> __(\"a\")\r\n"
				+ "true>>> lhs >>> __(\"b\")\r\n"
				+ "true>>> lhs >>> __(\"c\")\r\n"
				+ "true>>> lhs >>> __(\"d\")\r\n"
				+ "", sb.toString());
		
	}
	
	@Test public void testSpecialDefs() throws Exception {
		
		String input = IOUtils.toString(new FileReader("src/test/resources/japath3/core/m1-input.json"));
		
		Node n = w_(new JSONObject(input));
		
		String e = IOUtils.toString(new FileReader("src/test/resources/japath3/core/m4.ap"));
		
		PathExpr e_ = e_(e);
//		long t = System.currentTimeMillis();
		JSONObject jo = select(n, e_).val();
//		System.out.println(".. ms: " + (System.currentTimeMillis() - t));
		
		Testing.assertEquals_("specialDefs", jo.toString(3));
		
		
	}
	
	@Test 
//	@Ignore
	public void testRecursion() throws Exception {
		
		Node n = w_(" {a: false, b: 'lala', c: 99, d: null}  ");
		
		String e = IOUtils.toString(new FileReader("src/test/resources/japath3/core/recursion-1.ap"));
		
		@SuppressWarnings("unused")
		Object jo = select(n, e_(e)).val();
		
//		Testing.assertEquals_("specialDefs", jo.toString());
		
		
	}
	
	@Test 
//	@Ignore
	public void testStackFail() throws Exception {
		
		Node n = w_(" {a: 99 }  ");
		
		assertIt(n, "[x8899]", "def(f, g(88).g(#0)). def(g, js::conc($x, #0) $x). _{new $x:'x'}.f(99).$x");
		
	}
	
	public static void main(String[] args) throws Exception {
		
		JapathEnhTest t = new JapathEnhTest();
		
//		new Thread() {
//			@Override public void run() {
//				try {
//					t.testRecursion();
//				} catch (Exception e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			super.run(); 
//			}
//		}.start();
		
		Runnable r = new Runnable() {
			
			@Override public void run() { // TODO Auto-generated method stub
				while (true) {
					try {
						t.testStackFail();
						t.testSpecialDefs();
						Thread.sleep(20);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};
		
		new Thread(r).start();
		new Thread(r).start();
		
		
		
	}
	
}
