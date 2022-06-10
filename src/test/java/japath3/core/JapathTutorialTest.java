package japath3.core;

import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static japath3.core.Japath.walki;
import static japath3.processing.Language.e_;
import static japath3.wrapper.WJsonOrg.w_;

public class JapathTutorialTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void doEval() {

		JSONObject jo = new JSONObject(new JSONTokener(JapathTutorialTest.class.getResourceAsStream("healthcare.json")));

		for (Node node : walki(w_(jo), e_("identifier.period.start"))) {
			System.out.println(node);
		}
		for (Node node : walki(w_(jo), e_("identifier.period.*"))) {
			System.out.println(node);
		}
		for (Node node : walki(w_(jo), e_("name.*.given[1]"))) {
			System.out.println(node);
		}
		for (Node node : walki(w_(jo), e_("**.use"))) {
			System.out.println(node);
		}
		for (Node node : walki(w_(jo), e_("name.*.union(family, given).*"))) {
			System.out.println(node);
		}
		for (Node node : walki(w_(jo), e_("address.filter( postalCode.eq('3999') ).state"))) {
//		for (Node node : walki(w_(jo), e_("address ?( postalCode.eq('3999') ).state"))) {
			System.out.println(node);
		}
		
		jo = new JSONObject(new JSONTokener(JapathTutorialTest.class.getResourceAsStream("abc.json")));
		
		for (Node node : walki(w_(jo), e_("root.filter( " + 
				"	and(a.b,    // lalala \r\n" + 
				"	    a.c,    " + 
				"	    or( d.c.eq(11),  e.neq(88) ))" + 
				").d // lolo "))) {
			System.out.println(node);
		}
		
		jo = new JSONObject(new JSONTokener(JapathTutorialTest.class.getResourceAsStream("healthcare.json")));

		for (Node node : walki(w_(jo), e_(
				"cond(identifier.assigner.display.eq('Healthcare Ltd.'), "
				+ "   name.*.family, "
				+ "   name.*.given)"))) {
			System.out.println(node);
		}
		
		jo = new JSONObject(new JSONTokener(JapathTutorialTest.class.getResourceAsStream("books.json")));
		
		Vars vs = new Vars();
//		Var<JSONObject> b = vs.of("b");
//		Var<String> t = vs.of("t");
		Ctx ctx = new Ctx().setVars(vs);
		for (Node node : walki(w_(jo).setCtx(ctx), e_("root.store.book.* $b.title$t"))) {
			System.out.println(node);
			System.out.println(vs);
//			System.out.println(b.val());
//			System.out.println(t.val());
			System.out.println(vs.v("t").val());
			System.out.println(vs.v("b").val());
		}
		
	}

}
