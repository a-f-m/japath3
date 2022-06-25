package japath3.util;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.function.Consumer;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class BufferTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {}

	@Test
	public void test() {

		StringBuilder sb = new StringBuilder();

		Consumer<List<Integer>> puncher = l -> {
			sb.append(l.toString());
		};
		Buffer<Integer> buffer = new Buffer<Integer>(2, //
				puncher);

		buffer.add(1).add(2).add(3);
		buffer.flush();
		
		assertEquals("[1, 2][3]", sb.toString());
		
		sb.setLength(0);
		buffer.add(1, puncher);
		buffer.flush(puncher);
		
		assertEquals("[1]", sb.toString());
		
		sb.setLength(0);
		buffer.add(1, 2);
		buffer.flush();
		
		assertEquals("[1, 2][]", sb.toString());
		

	}

}
