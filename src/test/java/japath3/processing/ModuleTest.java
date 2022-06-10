package japath3.processing;

import static japath3.processing.Language.e_;
import static japath3.wrapper.WJsonOrg.w_;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import japath3.core.JapathException;
import japath3.core.Node;

public class ModuleTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void test() { 
		
		String s = "def(bcond, a.cond(b, b, c)) ";
		
		Module m = new Module("test", s);
		
		Node n = w_(" {a: {b: false, c: 'lala', d: 'lolo'} }  ");

		assertEquals("lala", m.trans(n, "bcond").val());
		
		// deferred TODO
//		assertEquals("lala", m.select(n, "a.bcond(b, b, c)").val());
//		assertEquals("lolo", m.select(n, "a.bcond(not(b), d, c)").val());
	}
	
	@Test public void testParams() {
		
		String s = "def(h, [#0, #1, #2, #3, #4]) .def(g, h(88, #0, #1, #2, #3)) .def(f, g(#0, a.c, #1, #2))";
		
		Module m = new Module("test", s);
		
		Node n = w_(" {a: {b: false, c: 'lala', d: 'lolo'} }  ");

		assertEquals("[88,77.1,\"lala\",\"lili\",true]", m.trans(n, "f", 77.1, "lili", true).val().toString());
		
		try {
			m.trans(n, "f", new JSONObject());
			fail();
		} catch (JapathException e) {
			// ok
		}
		
		s = "def(f, js::fconc('la', #0, #1))";
		
		m = new Module("test", s);
		
		n = w_(" {a: {b: false, c: 'lala', d: 'lolo'} }  ");
		
		assertEquals("lalala123", m.trans(n, "f", e_("a.c"), e_("union(1,2,3)")).val().toString());


	}
	
	// deferred
	
//	public static void main(String[] args) {
//		
//		String s = "def(bcond, cond(#0, #1, #2)) ";
//		
//		Module m = new Module(s);
//		
//		Node n = w_(" {a: {b: false, c: 'lala', d: 'lolo'} }  ");
//
//
//		
//		new Thread(new Runnable() {
//		    @Override
//		    public void run() {
//		   	 assertEquals("lala", m.select(n, "a.bcond(b, b, c)").val());
//		    }
//		}).start();
//		
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				assertEquals("lolo", m.select(n, "a.bcond(not(b), d, c)").val());
//			}
//		}).start();
//		
//	}

}
