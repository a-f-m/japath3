package japath3.processing;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import io.vavr.control.Option;
import japath3.core.Node;
import japath3.wrapper.WJsonOrg;

public class ModuleHandlerTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void test() throws Exception { 
	
		ModuleHandler mh = new ModuleHandler(new File("src/test/resources/japath3/processing/mod/mod-config-1.json"));
		
		int v = mh.getModule("m2").trans(Node.nil, "f").val();
		assertEquals(99, v);
		
		assertEquals(Option.none(), mh.getModule("s1").checkSchema(WJsonOrg.w_(88), "a"));
	}

}
