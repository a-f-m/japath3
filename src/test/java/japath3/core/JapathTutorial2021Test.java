package japath3.core;

import static japath3.core.Japath.select;
import static japath3.core.Japath.walki;
import static japath3.processing.Language.e_;
import static japath3.wrapper.WJsonOrg.w_;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import japath3.core.Japath.PathExpr;
import japath3.processing.Language;
import japath3.util.Regex;
import japath3.wrapper.WJsonOrg;

public class JapathTutorial2021Test {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void doEval() {

		JSONObject jo = new JSONObject(new JSONTokener(JapathTutorial2021Test.class.getResourceAsStream("person-1.json")));
		
		System.out.println(jo.toString(3));

		for (Node node : walki(w_(jo), e_("address.city"))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_("address.`postal-code`"))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_("address.*"))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_("telecom.*.value"))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_("telecom[0].value"))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_("address.*.§"))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_("**.use"))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_("address.union(city, `postal-code`)"))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_("telecom.*.?( use.eq('hidden') ).value" ))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_(
				"?(and(\r\n"
				+ "    age.lt(18),\r\n"
				+ "    driverLic .or(eq(3), eq(4))))\r\n"
				+ ".name"))) {
//		for (Node node : walki(w_(jo), e_("telecom.*.?( uste.eq('hidden') || use.eq('home')  ).value"))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_(
				"telecom.*"
				+ "  .?( value.match('.*5555 1.*') )"
				+ "  .use"))) {
			System.out.println(node.val().toString());
		}
		for (Node node : walki(w_(jo), e_(
				"shopping.*.*"
						+ "  .?( §.match('p#1.*') )"
						+ "  .status"))) {
			System.out.println(node.val().toString());
		}
//		for (Node node : walki(w_(jo), e_(
//				"shopping.*.*"
//						+ "  .?( status.eq('ordered'))"
//						+ "  .§"))) {
//			System.out.println(node.val().toString());
//		}
		for (Node node : walki(w_(jo), e_(
				"address"
				+ "   .cond( "
				+ "      absent, "
				+ "      `postal-code`, "
				+ "      city )"))) {
			System.out.println(node.val().toString());
		}
		// !!! note 'every' semantics
		for (Node node : walki(w_(jo), e_("cond( every( telecom.*.value, match('.*5555.*') ), name )  "))) {
			System.out.println(node.val().toString());
			System.out.println(new Ctx().getVars());
		}
		
		Vars vs = new Vars();
//		Var<String> c = vs.of("c");
//		Var<Boolean> p = vs.of("p");
		Ctx ctx = new Ctx().setVars(vs);
		for (@SuppressWarnings("unused") Node node : walki(w_(jo).setCtx(ctx), e_("address.?( and( city$c, `postal-code`$p ) )"))) {
			System.out.println(ctx.getVars());
//			System.out.println(c.val());
//			System.out.println(p.val());
		}
		
		for (Node node : walki(w_(jo), e_("?(true)"))) {
			System.out.println(node.val().toString());
			System.out.println(new Ctx().getVars());
		}

		
//		jo = new JSONObject(new JSONTokener(JapathTutorial2021Test.class.getResourceAsStream("abc.json")));
//		
//		for (Node node : walki(w_(jo), e_("root.filter( " + 
//				"	and(a.b,    // lalala \r\n" + 
//				"	    a.c,    " + 
//				"	    or( d.c.eq(11),  e.neq(88) ))" + 
//				").d  "))) {
//			System.out.println(node.val().toString());
//		}
//		
//		jo = new JSONObject(new JSONTokener(JapathTutorial2021Test.class.getResourceAsStream("healthcare.json")));
//
//		for (Node node : walki(w_(jo), e_(
//				"cond(identifier.assigner.display.eq('Healthcare Ltd.'), "
//				+ "   name.*.family, "
//				+ "   name.*.given)"))) {
//			System.out.println(node.val().toString());
//		}
//		
//		jo = new JSONObject(new JSONTokener(JapathTutorial2021Test.class.getResourceAsStream("books.json")));
//		
//		Vars vs = new Vars();
//		Var<JSONObject> b = vs.of("b");
//		Var<String> t = vs.of("t");
//		Ctx ctx = new Ctx().setVars(vs);
//		for (Node node : walki(w_(jo).setCtx(ctx), e_("root.store.book.*.$b.title.$t"))) {
//			System.out.println(node);
//			System.out.println(vs);
//			System.out.println(b.val());
//			System.out.println(t.val());
//			System.out.println(vs.v("t").val());
//		}

		@SuppressWarnings("unused")
		String s = 
		"// properties are multi-valued and a\r\n"
		+ "// text()-function selects element content. names of\r\n"
		+ "// element attributes have to be disjoint with element\r\n"
		+ "// names.\r\n"
		+ "\r\n"
		+ "root.*.*.book[#1].author.text()";
		
		System.out.println(Regex.check("p#a-b."));
		System.out.println("p#a-b".matches("p#a-b"));

	}

	@Test
	@Ignore
	public void doCacheSpeed() {
		
		long t = System.currentTimeMillis();
		for (int i = 0; i < 1000; i++) {
			e_(
					"address"
							+ "   .cond( "
							+ "      absent, "
							+ "      `postal-code`, "
							+ "      city )");
		}
//		System.out.println((System.currentTimeMillis() - t) / 1000d / 60d);
//		System.out.println((System.currentTimeMillis() - t) / 1000);
		System.out.println(System.currentTimeMillis() - t);
	}
	
	@Test
	public void doEvalWiki() {
		
		String jo = "{\r\n"
				+ "    \"pers\": {\r\n"
				+ "        \"name\": \"Miller\",\r\n"
				+ "        \"age\": 17,\r\n"
				+ "        \"#post-code\": 12205,\r\n"
				+ "        \"driverLic\": true\r\n"
				+ "    },\r\n"
				+ "    \"favs\": [\"coen-brothers\", \"dylan\"]\r\n"
				+ "}";
		
		Node node = WJsonOrg.w_(jo);
		
		PathExpr expr = Language.e_("pers.name");
		
		Node u = Japath.select(node, expr);
		String v = u.val();
		
		System.out.println(u);
		System.out.println(v);
		
		System.out.println(node);
		
		String v1 = Japath.select(w_(jo), e_("pers.name")).val();
		System.out.println(v1);
		
		System.out.println("---");
		
		Node person = select(w_(jo), e_("pers"));
		Node name = select(person, e_("name"));
		
		System.out.println(name.val().toString());
		
		JSONObject joPerson = person.val();
		
		System.out.println(joPerson);
		
		System.out.println("---");
		
		
		Iterable<Node> nodes = Japath.walki( w_(jo), e_("union( pers.*, favs.* )") );
		
		for (Node x : nodes) System.out.println(x.val().toString());
		
		System.out.println("---");
		
		Japath.walks(w_(jo), e_("union( pers.*, favs.* )")) //
				.filter(x -> {
					return x.val().toString().matches(".*(ill|bro).*");
				})
				.forEach(x -> System.out.println(x.val().toString()));

		System.out.println("---");

		Japath.walki(w_(jo), e_("union( pers.*, favs.* ) ? (match('.*(ill|bro).*') ) ")) //
				.forEach(x -> System.out.println(x.val().toString()));
	}

}
