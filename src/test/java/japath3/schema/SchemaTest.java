package japath3.schema;

import static japath3.util.JoeUtil.createJoe;
import static japath3.wrapper.NodeFactory.w_;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import japath3.core.Japath;
import japath3.core.Japath.Expr;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.processing.Language;
import japath3.schema.Schema.MessageHandling;
import japath3.util.Testing;
import japath3.wrapper.NodeFactory;


public class SchemaTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void testWriteSchema() throws Exception {

		// JSONObject jo = new JSONObject(
		// new JSONTokener(new
		// FileInputStream("src/test/resources/japath3/processing/schema-input-1.json")));
		String jo = 
				IOUtils.toString(new FileInputStream("src/test/resources/japath3/schema/schema-input-1.json"), "utf-8");

		Schema schema = new Schema().genSelectorRestriction(true);

		Expr e = schema.buildConstraintExpr(NodeFactory.w_(jo));

		System.out.println(e.toString());
//		assertEquals("and(path(all,sel,match('a|a1|b')),path(__(\"a\"),type(String)),path(__(\"a1\"),and(path(all,sel,match('a11|a22')),path(__(\"a11\"),type(Number)),path(__(\"a22\"),type(String)))),path(__(\"b\"),every(or(and(path(all,sel,match('c|d')),path(__(\"c\"),type(Number)),path(__(\"d\"),every(or(and(path(all,sel,match('Y|y')),path(__(\"Y\"),type(String)),path(__(\"y\"),type(Number))),and(path(all,sel,match('Z|y')),path(__(\"Z\"),type(String)),path(__(\"y\"),type(Number))),and(path(all,sel,match('end')),path(__(\"end\"),type(String))))))),and(path(all,sel,match('c|c_|f')),path(__(\"c\"),type(Number)),path(__(\"c_\"),type(Number)),path(__(\"f\"),every(and(path(all,sel,match('u|v')),path(__(\"u\"),type(String)),path(__(\"v\"),type(Number))))))))))", 
//				e.toString());
		
		String t = schema.buildConstraintText(NodeFactory.w_(jo));
		
		System.out.println(t);
//		assertEquals(e1, t + "\n");

		Writer fw = new BufferedWriter(
//				new OutputStreamWriter(new FileOutputStream("src/test/resources/japath3/schema/schema-input-1.jap"),
				new OutputStreamWriter(new FileOutputStream("src/test/resources/japath3/schema/schema-input-1.ap"),
						"utf-8"));

		fw. write(t);
		fw.close();
	}
	
	@Test public void testSchema() throws Exception {
		
		String jo =
				"{a:1, b:2}";
		
		Schema schema = new Schema().genSelectorRestriction(true);
		schema.setSchema( 
				"and(\n"
				+ "	every(*.§,\n"
				+ "		match('a|b')),\n"
				+ "	`a`.type(Number),\n"
				+ "	`b`.type(Number))");

		Node n = NodeFactory.w_(jo);
		System.out.println(schema.buildConstraintText(n));
		
		boolean valid = schema.checkValidity(n);
		
		System.out.println(schema.annotatedViolations(n));
		
		assertEquals(true, valid);
		
		//------
		
		schema.setSchema( 
				"and( "
				+ "	every(*, §.match('a|b')), "
				+ "	`a`.type(Number), "
				+ "	`b`.every(*, or( "
				+ "			and( "
				+ "				every(*, §.match('c')), "
				+ "				`c`.type(Number)), "
				+ "			and( "
				+ "				every(*, §.match('d')), "
				+ "				`d`.type(Number)))))");

		n = NodeFactory.w_("{a:'1', b:[{cx: 1}, {dx: '4'}]}");
		
		System.out.println(schema.buildConstraintText(n));
		valid = schema.checkValidity(n);
		
		assertEquals(false, valid);
		System.out.println(valid);
		
		String av = schema.annotatedViolations(n);
		
		System.out.println(av);
		
		Testing.assertEquals_("testSchema-1", av);
		
		schema.setSchema( 
				"and( "
				+ "	`a`.type(Number), "
				+ "	opt(`b`).every(*,  "
				+ "		or( "
				+ "			opt(`c`).type(Number), "
				+ "			opt(`d`).type(Number)))) "
				+ "");

		n = NodeFactory.w_("{a:1, b:[{cx: 3}, {dx: 4}]}");
		
//		System.out.println(schema.buildConstraintText(n));

		valid = schema.checkValidity(n);
		
		System.out.println(schema.annotatedViolations(n));
		assertEquals(true, valid);
	}
	
	@Test public void testCompleteness() throws Exception {
		
		String jo =
				"{a:1, b:{cx:1, d:'lolo', d1:'lolo'}}";
		
		Schema schema = new Schema().genCompleteness(true);
		schema.setSchema( 
				"_{**.::complete}."
				+ "and(\n"
				+ "	a.type(Number),\n"
				+ "	b"
//				+ "     {d:complete}"
				+ "     .and(\n"
				+ "		  c.type(Number),\n"
				+ "		  d.type(String)))\n"
				+ "");

//		NodeFactory.test = true;
		Node n = NodeFactory.w_(jo);
//		System.out.println(schema.buildConstraintText(n));
		
		boolean valid = schema.checkValidity(n);
		
//		System.out.println(schema.annotatedViolations(n));
		Testing.assertEquals_("testCompleteness-1", schema.annotatedViolations(n));
		assertEquals(false, valid);
		
		assertEquals(
				"LinkedHashMap(({\"a\":1,\"b\":{\"cx\":1,\"d\":\"lolo\",\"d1\":\"lolo\"}}, HashSet(a, b)), (1, HashSet()), ({\"cx\":1,\"d\":\"lolo\",\"d1\":\"lolo\"}, HashSet(c, d)), (lolo, HashSet()))",
				schema.propHits.toString());
//		NodeFactory.test = false;
	}
	
	@Test public void testMessage() throws Exception {
		
		Schema schema = new Schema().genMessages(MessageHandling.Only);
		schema.setSchema( 				
				"""						
				_{**.::complete}.
				   assert(
				   	a. assert(message('num exp'), type(Number)),
				   	b
				        .assert(
				   		  message('c, d must exist and of type Number resp. String'),
				   		  c.type(Number),
				   		  d.type(String)))
				"""
		);
		
		Node n = w_("{a:1, b:{cx:1, d:'lolo', d1:'lolo'}}");
		boolean valid = schema.checkValidity(n);
		
		assertEquals(false, valid);
		Testing.assertEquals_("testMessage-1", schema.annotatedViolations(n));
		
		n = w_("{a:'lili', b:{cx:1, d:'lolo', d1:'lolo'}}");
		schema.genMessages(MessageHandling.Prefer);
		schema.checkValidity(n);
		Testing.assertEquals_("testMessage-2", schema.annotatedViolations(n));
	}
	
	@Test
	@Ignore
	public void testNulls() throws Exception {
		
		Node n = NodeFactory.w_(createJoe("{a: 1, b: null, c: [1, null]}"));
		System.out.println(n);
		
//		Schema schema = new Schema().genCompleteness(true);
		
//		schema.setSchema( 
//				+ "and(\n"
//				+ "	a.type(Number),\n"
//				+ "	b.neq(null),"
////				+ "     {d:complete}"
//				+ "     .and(\n"
//				+ "		  c.type(Number),\n"
//				+ "		  d.type(String)))\n"
//				+ "");

		
//		boolean valid = schema.checkValidity(n);
	}


	@Test public void testSchemaFile() throws Exception {

		String jo = 
				IOUtils.toString(new FileInputStream("src/test/resources/japath3/schema/schema-input-1.json"), "utf-8");

		System.out.println("------");
		
		Schema schema = new Schema().genSelectorRestriction(true)
				.setSchema(IOUtils.toString(new FileInputStream("src/test/resources/japath3/schema/schema-input-1.ap"), "utf-8"));

		Node n = NodeFactory.w_(jo);
		System.out.println(schema.buildConstraintText(n));
		boolean valid = schema.checkValidity(n);
		System.out.println(schema.annotatedViolations(n));
		assertEquals(true, valid);
	}
	
	@Test public void testSalience() throws Exception {
		
		Node n = NodeFactory.w_("{a:1, b:[{cx: 3}, {dx: 4}]}");
		
		
		try {
			n.ctx.setSalient(true);
			Japath.select(n, Language.e_("**.cxx"));

		} catch (JapathException e) {
			assertEquals(
					"japath3.core.JapathException: salience: selectors [cxx] used but not found (available seletors: [a, b, cx, dx])",
					e.toString());
		}
		assertEquals("TreeSet(, a, b, cx, dx)", n.ctx.defSelectors.toString());
		assertEquals("TreeSet(cxx)", n.ctx.undefSelectors.toString());
	}
	
	@Test public void testNegation() throws Exception {
		
		Node n = w_("{a:1, b:[{c: 88, cx: 3}, {c: 88, cx: 4}]}");
		
		assertTrue(Schema.checkValidity(n, "assert(not(b[0].cx.eq(3)))").length() != 0);

		assertTrue(Schema.checkValidity(n, "assert(b[0].cx.eq(4))").length() != 0);
		
		assertEquals(null, Schema.checkValidity(n, "assert(b[0].cx.eq(3))"));
		
//		assertEquals(null, Schema.checkValidity(n, "assert(cond(a, assert(not(b.some(*, and(c.eq(88), or(cx.eq(5), not(cx))))))))"));
//		assertEquals(expect2, Schema.checkValidity(n, "assert(cond(a, assert(not(b.some(*, and(c.eq(88), cx.eq(4)))))))").toString());
		Testing.assertEquals_("testNegation-1",
				Schema.checkValidity(n, "assert(cond(a, assert(not(b.some(*, and(c.eq(88), cx.eq(4)))))))").toString());

//		assertEquals(null, Schema.checkValidity(w_(new JSONObject(json1)), e2));
	}
	
	@SuppressWarnings("unused")
	private String cr(String s) { return s.replace("\r", ""); }

	String expect1 = "{  // !!! possible correction: b.every(*,or(assert(every(*,&.match('c')),c.type(Number)),assert(every(*,&.match('d')),d.type(Number))))\n"
			+ "  // !!! possible correction: a.type(Number)\n"
			+ "\n"
			+ "  \"a\": \"1\",    // !!! possible correction: type(Number)\n"
			+ "\n"
			+ "  \"b\": [\n"
			+ "    {      // !!! possible correction: every(*,or(assert(every(*,&.match('c')),c.type(Number)),assert(every(*,&.match('d')),d.type(Number))))\n"
			+ "      // !!! possible correction: or(assert(every(*,&.match('c')),c.type(Number)),assert(every(*,&.match('d')),d.type(Number)))\n"
			+ "      // !!! possible correction: d.type(Number)\n"
			+ "      // !!! possible correction: every(*,&.match('d'))\n"
			+ "      // !!! possible correction: c.type(Number)\n"
			+ "      // !!! possible correction: every(*,&.match('c'))\n"
			+ "\n"
			+ "      \"cx\": 1        // !!! possible correction: every(*,&.match('d'))\n"
			+ "        // !!! possible correction: every(*,&.match('c'))\n"
			+ "\n"
			+ "    },\n"
			+ "    {      // !!! possible correction: every(*,or(assert(every(*,&.match('c')),c.type(Number)),assert(every(*,&.match('d')),d.type(Number))))\n"
			+ "      // !!! possible correction: or(assert(every(*,&.match('c')),c.type(Number)),assert(every(*,&.match('d')),d.type(Number)))\n"
			+ "      // !!! possible correction: d.type(Number)\n"
			+ "      // !!! possible correction: every(*,&.match('d'))\n"
			+ "      // !!! possible correction: c.type(Number)\n"
			+ "      // !!! possible correction: every(*,&.match('c'))\n"
			+ "\n"
			+ "      \"dx\": \"4\"        // !!! possible correction: every(*,&.match('d'))\n"
			+ "        // !!! possible correction: every(*,&.match('c'))\n"
			+ "\n"
			+ "    }\n"
			+ "  ]\n"
			+ "}\n"
			+ "";
	
	String expect2 = "{  // !!! possible correction: cond(a,assert(not(b.some(*,assert(c.eq(88),cx.eq(4))))))\n"
			+ "  // !!! possible correction: not(b.some(*,assert(c.eq(88),cx.eq(4))))\n"
			+ "\n"
			+ "  \"a\": 1,\n"
			+ "  \"b\": [\n"
			+ "    {\n"
			+ "      \"c\": 88,\n"
			+ "      \"cx\": 3\n"
			+ "    },\n"
			+ "    {\n"
			+ "      \"c\": 88,\n"
			+ "      \"cx\": 4\n"
			+ "    }\n"
			+ "  ]\n"
			+ "}\n"
			+ ""; 
	
	String e1 = "assert(\n"
			+ "	every(*,\n"
			+ "		&.match('a|a1|b')),\n"
			+ "	a.type(String),\n"
			+ "	a1.assert(\n"
			+ "		every(*,\n"
			+ "			§.match('a11|\\Qa22\"-§$%\\E')),\n"
			+ "		a11.type(Number),\n"
			+ "		`a22\"-§$%`.type(String)),\n"
			+ "	b.every(*,\n"
			+ "		or(\n"
			+ "			assert(\n"
			+ "				every(*,\n"
			+ "					§.match('c|d')),\n"
			+ "				c.type(Number),\n"
			+ "				d.every(*,\n"
			+ "					or(\n"
			+ "						assert(\n"
			+ "							every(*,\n"
			+ "								§.match('Y|y')),\n"
			+ "							Y.type(String),\n"
			+ "							y.type(Number)),\n"
			+ "						assert(\n"
			+ "							every(*,\n"
			+ "								§.match('Z|y')),\n"
			+ "							Z.type(String),\n"
			+ "							y.type(Number)),\n"
			+ "						assert(\n"
			+ "							every(*,\n"
			+ "								§.match('end')),\n"
			+ "							end.type(String))))),\n"
			+ "			assert(\n"
			+ "				every(*,\n"
			+ "					§.match('c|c_|f')),\n"
			+ "				c.type(Number),\n"
			+ "				c_.type(Number),\n"
			+ "				f.every(*,\n"
			+ "					or(\n"
			+ "						type(String),\n"
			+ "						assert(\n"
			+ "							every(*,\n"
			+ "								§.match('u|v')),\n"
			+ "							u.type(String),\n"
			+ "							v.type(Number))))))))\n"
			+ "";
	
	String json1 = "";
	
	String e2 = "";
	
}
