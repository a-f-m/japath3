package japath3.util;

import static org.junit.Assert.assertEquals;

import java.util.Iterator;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import japath3.util.Forker.Fork;

public class ForkerTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void test() { 
		
		Fork<?, Integer> f1 = (f, x) -> {
			return new Iterator<Integer>() {
				int cnt = 0;
				@Override public boolean hasNext() { return cnt < 3; }

				@Override public Integer next() { 
					return ++cnt; 
				}
			};
		};

		Fork<Integer, Double> f2 = (f, x) -> {
			return java.util.List.of(x / 2D, x / 3D).iterator();
		};

		Fork<Double, String> f3 = (f, x) -> {
			return Forker.single("yes: " + x);
		};

		List<Fork> ee = List.of( f1, f2, f3 );
		
//		System.out.println(List.of(ee).toString());

		String res = "";
		long t = System.currentTimeMillis();
		int i = 0;
		for (Iterator<String> iterator = new Forker<String>(ee).eval(); iterator.hasNext();) {
			String next = iterator.next();
			i++;
			res += next + " ";
			if (i % 10000 == 0) {
//				System.out.println(next);
			}
		}
		assertEquals("yes: 0.5 yes: 0.3333333333333333 yes: 1.0 yes: 0.6666666666666666 yes: 1.5 yes: 1.0 ", res);
		
		System.out.println(i);
		System.out.println(".. ms: " + (System.currentTimeMillis() - t));

		
	}

}
