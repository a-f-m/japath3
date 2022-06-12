package japath3.processing;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static japath3.core.Japath.__;
import static japath3.core.Japath.all;
import static japath3.core.Japath.and;
import static japath3.core.Japath.bind;
import static japath3.core.Japath.desc;
import static japath3.core.Japath.eq;
import static japath3.core.Japath.every;
import static japath3.core.Japath.filter;
import static japath3.core.Japath.or;
import static japath3.core.Japath.path;
import static japath3.core.Japath.type;
import static japath3.core.Japath.union;
import static japath3.util.Basics.prettyNesting;
import static org.junit.Assert.assertEquals;

import io.vavr.Tuple2;
import japath3.core.Japath.BoolExpr;
import japath3.core.Japath.Expr;
import japath3.core.Japath.PathExpr;
import japath3.core.Var;

public class LanguageTest extends Language {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}
	
	@Test public void testEscaping() {
		
		String input = 
"\"x \\\" \\a \\\" x\".  eq('y  \\' \\a \\' y').  `z \\` \\a / \\` z`";
		
		System.out.println(input);
		Tuple2<JSONObject, String> ast = Language.getAst(input);
		if (ast._1 == null) {
			System.out.println(ast._2.toString());
			return;
		}
		
		System.out.println(ast._1.toString(1));
		
		Expr e = e_(input);
		
		System.out.println(stringify(e, 1));
		
		assertEquals(stringify(e, 1), stringify(Language.clone(e, false), 1));
		
	}

	@Test public void testLang() { 
		
		String input = "or(`x \" \"`, imply(a, b)). some(*, c). b$v.*. a[1][2][#8]. a[#1..]. a[#1..2] .?(a). x?(a). `c\\` \\` §$` . [a, b]. union(or(a, b), union(or(c, d)))."
				+ "**. * .union ( a, b ). $ x .* . eq ( 88 ). eq ( -1.44E11 ). eq ( \"lolo\" ). eq ( $x ). §. match('la\\'la'). cond(a, b, c). self. text(). "
				+ "def(func, #1.a.b). func(x, y.z) { \"a\" :(c.d)}. a {x.y, z}. java::sys::func(a, b.c). {c : 0}. j::sys::func(). js::func(). ::complete"
				+ ". def-script(\"\"\"function aaa(){ \n return bbb}\"\"\"). property(a.b). message('lala')"  
				;
		
		Tuple2<JSONObject, String> ast = Language.getAst(input);
		System.out.println(ast._2);
		System.out.println(ast._1.toString(3));
		assertEquals(astA.replace("\r", ""), ast._1.toString(3));
		
		Expr e = e_(input);
		
		assertEquals(stringify(e, 1), stringify(Language.clone(e, false), 1));
		
		assertEquals(es.replace("\r", ""), prettyNesting(stringify(e, 1), "\\,|\\.", ""));
//		assertEquals(es.replace("\r", ""), stringify(e, 1));
//		System.out.println(es);
		
		// check exception
		e_(es);
		
		Var x = null;
		PathExpr path = path(or(__("x"), __("y")),
				__("b"),
				all,
				__(7),
				filter(__("a")),
				__("c §$"),
				union(or(__("a"), __("b")), union(or(__("c"), __("d")))),
				desc,
				all,
				union(__("a"), __("b")),
				bind(x),
				all,
				eq(88),
				eq("lolo"));

		assertEquals(esB.replace("\r", ""), prettyNesting(stringify(path, 1), "\\,|\\.", ""));
		
		@SuppressWarnings("unused")
		BoolExpr and = //
				and(path(__("a"), type("String")),
						path(__("a1"), and(path(__("a11"), type("Number")), path(__("a22"), type("String")))),
						path(__("b"),
								every(all,
										or(and(path(__("c"), type("Number")),
												path(__("d"),
														every(all,
																or(and(path(__("Y"), type("String")),
																		path(__("y"), type("Number"))),
																		and(path(__("Z"), type("String")),
																				path(__("y"), type("Number"))),
																		path(__("end"), type("String")))))),
												and(path(__("c"), type("Number")),
														path(__("c_"), type("Number")),
														path(__("f"),
																every(all,
																		and(path(__("u"), type("String")),
																				path(__("v"), type("Number"))))))))));

	}
	
	String es = "or(\r\n"
			+ "	`x \" \"`,\r\n"
			+ "	imply(\r\n"
			+ "		a,\r\n"
			+ "		b)).\r\n"
			+ "some(\r\n"
			+ "	*,\r\n"
			+ "	c).\r\n"
			+ "b $v.\r\n"
			+ "*.\r\n"
			+ "a [1] [2] [#8].\r\n"
			+ "a [#1..].\r\n"
			+ "a [#1..2] ?(\r\n"
			+ "	a).\r\n"
			+ "x ?(\r\n"
			+ "	a).\r\n"
			+ "`c\\` \\` §$`.\r\n"
			+ "[a,\r\n"
			+ "b].\r\n"
			+ "union(\r\n"
			+ "	or(\r\n"
			+ "		a,\r\n"
			+ "		b),\r\n"
			+ "	union(\r\n"
			+ "		or(\r\n"
			+ "			c,\r\n"
			+ "			d))).\r\n"
			+ "**.\r\n"
			+ "*.\r\n"
			+ "union(\r\n"
			+ "	a,\r\n"
			+ "	b).\r\n"
			+ "$x.\r\n"
			+ "*.\r\n"
			+ "eq(\r\n"
			+ "	88).\r\n"
			+ "eq(\r\n"
			+ "	-144000000000).\r\n"
			+ "eq(\r\n"
			+ "	'lolo').\r\n"
			+ "eq(\r\n"
			+ "	$x).\r\n"
			+ "&.\r\n"
			+ "match(\r\n"
			+ "	'la\\'la').\r\n"
			+ "cond(\r\n"
			+ "	a,\r\n"
			+ "	b,\r\n"
			+ "	c).\r\n"
			+ "self.\r\n"
			+ "text(\r\n"
			+ "	).\r\n"
			+ "def(\r\n"
			+ "	func,\r\n"
			+ "	#1.\r\n"
			+ "	a.\r\n"
			+ "	b).\r\n"
			+ "func(\r\n"
			+ "	x,\r\n"
			+ "	y.\r\n"
			+ "	z) {\r\n"
			+ "	a : (\r\n"
			+ "		c.\r\n"
			+ "		d)}.\r\n"
			+ "a {\r\n"
			+ "	x.\r\n"
			+ "	y,\r\n"
			+ "	z}.\r\n"
			+ "java::sys::func(\r\n"
			+ "	a,\r\n"
			+ "	b.\r\n"
			+ "	c).\r\n"
			+ "{\r\n"
			+ "	c : (\r\n"
			+ "		0)}.\r\n"
			+ "java::sys::func(\r\n"
			+ "	).\r\n"
			+ "javascript::func(\r\n"
			+ "	).\r\n"
			+ "::complete(\r\n"
			+ "	).\r\n"
			+ "property(\r\n"
			+ "	a.\r\n"
			+ "	b).\r\n"
			+ "message(\r\n"
			+ "	'lala')";
	
	String esB = "or(\r\n"
			+ "	x,\r\n"
			+ "	y).\r\n"
			+ "b.\r\n"
			+ "* [7] ?(\r\n"
			+ "	a).\r\n"
			+ "`c §$`.\r\n"
			+ "union(\r\n"
			+ "	or(\r\n"
			+ "		a,\r\n"
			+ "		b),\r\n"
			+ "	union(\r\n"
			+ "		or(\r\n"
			+ "			c,\r\n"
			+ "			d))).\r\n"
			+ "**.\r\n"
			+ "*.\r\n"
			+ "union(\r\n"
			+ "	a,\r\n"
			+ "	b).\r\n"
			+ "bind(\r\n"
			+ "	null,\r\n"
			+ "	null).\r\n"
			+ "*.\r\n"
			+ "eq(\r\n"
			+ "	88).\r\n"
			+ "eq(\r\n"
			+ "	'lolo')";
	
	String astA = "{\"start\": {\"path\": [\r\n"
			+ "   {\"step\": {\"boolExpr\": {\r\n"
			+ "      \"op\": \"or\",\r\n"
			+ "      \"args\": [\r\n"
			+ "         {\"path\": [{\"step\": {\"property\": \"x \\\" \\\"\"}}]},\r\n"
			+ "         {\"path\": [{\"step\": {\"boolExpr\": {\r\n"
			+ "            \"op\": \"imply\",\r\n"
			+ "            \"args\": [\r\n"
			+ "               {\"path\": [{\"step\": {\"property\": \"a\"}}]},\r\n"
			+ "               {\"path\": [{\"step\": {\"property\": \"b\"}}]}\r\n"
			+ "            ]\r\n"
			+ "         }}}]}\r\n"
			+ "      ]\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"quantifierExpr\": {\r\n"
			+ "      \"op\": \"some\",\r\n"
			+ "      \"quant\": {\"path\": [{\"step\": {\"wild\": \"all\"}}]},\r\n"
			+ "      \"check\": {\"path\": [{\"step\": {\"property\": \"c\"}}]}\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"property\": \"b\"}},\r\n"
			+ "   {\"step\": {\"bind\": \"v\"}},\r\n"
			+ "   {\"step\": {\"wild\": \"all\"}},\r\n"
			+ "   {\"step\": {\"property\": \"a\"}},\r\n"
			+ "   {\"subscript\": 1},\r\n"
			+ "   {\"subscript\": 2},\r\n"
			+ "   {\r\n"
			+ "      \"subscript\": 8,\r\n"
			+ "      \"seq\": true\r\n"
			+ "   },\r\n"
			+ "   {\"step\": {\"property\": \"a\"}},\r\n"
			+ "   {\r\n"
			+ "      \"subscript\": 1,\r\n"
			+ "      \"seq\": true,\r\n"
			+ "      \"upper\": -1\r\n"
			+ "   },\r\n"
			+ "   {\"step\": {\"property\": \"a\"}},\r\n"
			+ "   {\r\n"
			+ "      \"subscript\": 1,\r\n"
			+ "      \"seq\": true,\r\n"
			+ "      \"upper\": 2\r\n"
			+ "   },\r\n"
			+ "   {\"step\": {\"filter\": {\"path\": [{\"step\": {\"property\": \"a\"}}]}}},\r\n"
			+ "   {\"step\": {\"property\": \"x\"}},\r\n"
			+ "   {\"filter\": {\"path\": [{\"step\": {\"property\": \"a\"}}]}},\r\n"
			+ "   {\"step\": {\"property\": \"c` ` §$\"}},\r\n"
			+ "   {\"step\": {\"array\": {\"args\": [\r\n"
			+ "      {\"path\": [{\"step\": {\"property\": \"a\"}}]},\r\n"
			+ "      {\"path\": [{\"step\": {\"property\": \"b\"}}]}\r\n"
			+ "   ]}}},\r\n"
			+ "   {\"step\": {\"union\": {\r\n"
			+ "      \"arrayFlag\": false,\r\n"
			+ "      \"args\": [\r\n"
			+ "         {\"path\": [{\"step\": {\"boolExpr\": {\r\n"
			+ "            \"op\": \"or\",\r\n"
			+ "            \"args\": [\r\n"
			+ "               {\"path\": [{\"step\": {\"property\": \"a\"}}]},\r\n"
			+ "               {\"path\": [{\"step\": {\"property\": \"b\"}}]}\r\n"
			+ "            ]\r\n"
			+ "         }}}]},\r\n"
			+ "         {\"path\": [{\"step\": {\"union\": {\r\n"
			+ "            \"arrayFlag\": false,\r\n"
			+ "            \"args\": [{\"path\": [{\"step\": {\"boolExpr\": {\r\n"
			+ "               \"op\": \"or\",\r\n"
			+ "               \"args\": [\r\n"
			+ "                  {\"path\": [{\"step\": {\"property\": \"c\"}}]},\r\n"
			+ "                  {\"path\": [{\"step\": {\"property\": \"d\"}}]}\r\n"
			+ "               ]\r\n"
			+ "            }}}]}]\r\n"
			+ "         }}}]}\r\n"
			+ "      ]\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"wild\": \"desc\"}},\r\n"
			+ "   {\"step\": {\"wild\": \"all\"}},\r\n"
			+ "   {\"step\": {\"union\": {\r\n"
			+ "      \"arrayFlag\": false,\r\n"
			+ "      \"args\": [\r\n"
			+ "         {\"path\": [{\"step\": {\"property\": \"a\"}}]},\r\n"
			+ "         {\"path\": [{\"step\": {\"property\": \"b\"}}]}\r\n"
			+ "      ]\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"var\": \"x\"}},\r\n"
			+ "   {\"step\": {\"wild\": \"all\"}},\r\n"
			+ "   {\"step\": {\"compare\": {\r\n"
			+ "      \"op\": \"eq\",\r\n"
			+ "      \"arg\": {\"constant\": 88}\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"compare\": {\r\n"
			+ "      \"op\": \"eq\",\r\n"
			+ "      \"arg\": {\"constant\": -144000000000}\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"compare\": {\r\n"
			+ "      \"op\": \"eq\",\r\n"
			+ "      \"arg\": {\"constant\": \"lolo\"}\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"compare\": {\r\n"
			+ "      \"op\": \"eq\",\r\n"
			+ "      \"arg\": {\"path\": [{\"step\": {\"var\": \"x\"}}]}\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"selector\": [\"§\"]}},\r\n"
			+ "   {\"step\": {\"compare\": {\r\n"
			+ "      \"op\": \"match\",\r\n"
			+ "      \"arg\": {\"constant\": \"la'la\"}\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"conditional\": {\r\n"
			+ "      \"cond\": {\"path\": [{\"step\": {\"property\": \"a\"}}]},\r\n"
			+ "      \"ifExpr\": {\"path\": [{\"step\": {\"property\": \"b\"}}]},\r\n"
			+ "      \"elseExpr\": {\"path\": [{\"step\": {\"property\": \"c\"}}]}\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"self\": \"\"}},\r\n"
			+ "   {\"step\": {\"text\": \"\"}},\r\n"
			+ "   {\"step\": {\"def\": {\r\n"
			+ "      \"name\": \"func\",\r\n"
			+ "      \"expr\": {\"path\": [\r\n"
			+ "         {\"step\": {\"argNumber\": 1}},\r\n"
			+ "         {\"step\": {\"property\": \"a\"}},\r\n"
			+ "         {\"step\": {\"property\": \"b\"}}\r\n"
			+ "      ]}\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"exprAppl\": {\r\n"
			+ "      \"name\": \"func\",\r\n"
			+ "      \"args\": [\r\n"
			+ "         {\"path\": [{\"step\": {\"property\": \"x\"}}]},\r\n"
			+ "         {\"path\": [\r\n"
			+ "            {\"step\": {\"property\": \"y\"}},\r\n"
			+ "            {\"step\": {\"property\": \"z\"}}\r\n"
			+ "         ]}\r\n"
			+ "      ]\r\n"
			+ "   }}},\r\n"
			+ "   {\"subExpr\": {\"args\": [{\"assignment\": {\r\n"
			+ "      \"lhs\": {\"step\": {\"lhsProperty\": \"a\"}},\r\n"
			+ "      \"rhs\": {\"path\": [\r\n"
			+ "         {\"step\": {\"property\": \"c\"}},\r\n"
			+ "         {\"step\": {\"property\": \"d\"}}\r\n"
			+ "      ]}\r\n"
			+ "   }}]}},\r\n"
			+ "   {\"step\": {\"property\": \"a\"}},\r\n"
			+ "   {\"subExpr\": {\"args\": [\r\n"
			+ "      {\"path\": [\r\n"
			+ "         {\"step\": {\"property\": \"x\"}},\r\n"
			+ "         {\"step\": {\"property\": \"y\"}}\r\n"
			+ "      ]},\r\n"
			+ "      {\"path\": [{\"step\": {\"property\": \"z\"}}]}\r\n"
			+ "   ]}},\r\n"
			+ "   {\"step\": {\"funcCall\": {\r\n"
			+ "      \"kind\": \"java\",\r\n"
			+ "      \"ns\": \"sys\",\r\n"
			+ "      \"func\": \"func\",\r\n"
			+ "      \"args\": [\r\n"
			+ "         {\"path\": [{\"step\": {\"property\": \"a\"}}]},\r\n"
			+ "         {\"path\": [\r\n"
			+ "            {\"step\": {\"property\": \"b\"}},\r\n"
			+ "            {\"step\": {\"property\": \"c\"}}\r\n"
			+ "         ]}\r\n"
			+ "      ]\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"struct\": {\"args\": [{\"assignment\": {\r\n"
			+ "      \"lhs\": {\"path\": [{\"step\": {\"property\": \"c\"}}]},\r\n"
			+ "      \"rhs\": {\"constant\": 0}\r\n"
			+ "   }}]}}},\r\n"
			+ "   {\"step\": {\"funcCall\": {\r\n"
			+ "      \"kind\": \"java\",\r\n"
			+ "      \"ns\": \"sys\",\r\n"
			+ "      \"func\": \"func\"\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"funcCall\": {\r\n"
			+ "      \"kind\": \"javascript\",\r\n"
			+ "      \"ns\": \"\",\r\n"
			+ "      \"func\": \"func\"\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"funcCall\": {\r\n"
			+ "      \"kind\": \"directive\",\r\n"
			+ "      \"ns\": \"\",\r\n"
			+ "      \"func\": \"complete\"\r\n"
			+ "   }}},\r\n"
			+ "   {\"step\": {\"defScript\": {\"s\": \"function aaa(){ \\\\n return bbb}\"}}},\r\n"
			+ "   {\"step\": {\"pathAsProperty\": {\"path\": [\r\n"
			+ "      {\"step\": {\"property\": \"a\"}},\r\n"
			+ "      {\"step\": {\"property\": \"b\"}}\r\n"
			+ "   ]}}},\r\n"
			+ "   {\"step\": {\"message\": {\"constant\": \"lala\"}}}\r\n"
			+ "]}}";
}
