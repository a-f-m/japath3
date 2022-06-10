package japath3.util;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vavr.PartialFunction;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.List;

public class JoeUtilTest extends JoeUtil {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {}
	
	interface X extends List<Tuple2<Integer, List<Object>>> {
		
	}

	@Test
	public void test() {

		Object o1 = new Object();
		Object o2 = new Object();
		Object dumm = new Object();

		List<Tuple2<Integer, List<Object>>> l = List.of( //
				Tuple.of(1, List.of(dumm)), //
				Tuple.of(2, List.of(o1)), //
				Tuple.of(3, List.of(o1, o2)));

		List<Tuple2<Integer, List<Object>>> coll =
				l.collect(new PartialFunction() {

					@Override
					public Object apply(Object t) { 
					return t; }

					@Override
					public boolean isDefinedAt(Object value) { 
						return ((Tuple2<Integer, List<Object>>) value)._2.contains(o2); }

//					@Override
//					public Tuple2<Integer, List<Object>> apply(Tuple2<Integer, List<Object>> t) { return t; }
//
//					@Override
//					public boolean isDefinedAt(Tuple2<Integer, List<Object>> value) {
//						
//						return value._2.contains(o2);
//					}

				});

		System.out.println(coll.toString());
	}
}
