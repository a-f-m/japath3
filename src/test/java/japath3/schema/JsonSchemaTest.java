package japath3.schema;

import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import dev.harrel.jsonschema.ValidatorFactory;
import dev.harrel.jsonschema.providers.GsonNode;
import japath3.core.Node;
import japath3.util.Testing;
import japath3.wrapper.NodeFactory;

public class JsonSchemaTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void test() { 
		
		String schema = """
		        {
		          "type": "boolean"
		        }""";
		String instance = "true";
		boolean valid = new ValidatorFactory().withJsonNodeFactory(new GsonNode.Factory()).validate(schema, instance).isValid();
		assertTrue(valid);
	}
	
	@Test public void test1() {
		
		Node jo = NodeFactory.w_(
				"""
				{
				   "pers": {
				      "name": "Miller",
				      "age": 17,
				      "#post-code": 12205,
				      "driverLic": true,
				      "null": null,
				      "embedded":  {"x" : 1}
				   },
				   "favs": [
				      "coen-brothers",
				      "dylan",
				      {"uups": "an struct"},
				      {"embedded":  {"x" : 1}}
				   ]
				}				
				"""
				);
		
		Node jschema = new JsonSchema().setOpt(false).setModular(true).buildJsonTopSchema(jo);
		jschema.remove("$id");
		System.out.println(jschema.woString(3));
		
		Testing.assertEquals_(getClass(), "1", jschema.woString(3));
		
	}

}
