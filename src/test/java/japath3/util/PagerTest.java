package japath3.util;

import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyIterator;
import static org.junit.Assert.assertEquals;

import japath3.util.Pager.PageFunc;

public class PagerTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {}

	@Test
	public void test() {
		
		StringBuffer sb = new StringBuffer();

		List<Integer> l = asList(1, 2, 3, 4);

		PageFunc<Integer> func = (offs, limit) -> {
			int to = min(offs + limit, l.size());
			return to <= offs ? emptyIterator() : l.subList(offs, to).iterator();
		};

		Pager<Integer> pww = new Pager(2, func);
		for (Integer i : pww) sb.append(i);
		assertEquals("1234", sb.toString());

		sb.setLength(0);
		pww = new Pager(3, func);
		for (Integer i : pww) sb.append(i);
		assertEquals("1234", sb.toString());
		
		sb.setLength(0);
		pww = new Pager(6, func);
		for (Integer i : pww) sb.append(i);
		assertEquals("1234", sb.toString());
	}

}
