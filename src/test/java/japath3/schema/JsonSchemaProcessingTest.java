package japath3.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.FileReader;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import dev.harrel.jsonschema.Error;
import dev.harrel.jsonschema.Validator;
import dev.harrel.jsonschema.ValidatorFactory;
import dev.harrel.jsonschema.providers.GsonNode;
import io.vavr.control.Option;
import japath3.core.Node;
import japath3.util.Testing;
import japath3.wrapper.NodeFactory;
import japath3.wrapper.WGson;

public class JsonSchemaProcessingTest {
	
	static Class h;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
		h = NodeFactory.getDefaultWrapperClass();
		NodeFactory.setDefaultWrapperClass(WGson.class);
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
		NodeFactory.setDefaultWrapperClass(h);		
	}

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
				{"$defs":{
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
				}}	
				"""
				);
		
		Node jschema = new JsonSchemaProcessing().setOpt(false).setModular(true).setOnlyTopModular(false).buildJsonTopSchema(jo);
		jschema.remove("$id");
		System.out.println(jschema.woString(3));
		
		Testing.assertEquals_(getClass(), "test1", jschema.woString(3));
		
	}
	
	@Test public void testSchemaBundle() throws Exception {
		
		Node bundle = NodeFactory
				.w_(IOUtils.toString(new FileReader("src\\test\\resources\\japath3\\schema\\prototypes-1.jsonc")));
		
//		System.out.println(bundle.woString(3));
		
		JsonSchemaProcessing js = new JsonSchemaProcessing().setModular(true).setOnlyTopModular(false);
		
		Node topSchema = js.buildJsonTopSchema(bundle);
		System.out.println(topSchema.woString(3));
		
		Testing.assertEquals_(getClass(), "testSchemaBundle-1", topSchema.woString(3));
		
		ValidatorFactory validator = new ValidatorFactory().withJsonNodeFactory(new GsonNode.Factory());
		
		Validator.Result result = validator.validate(topSchema.woString(3), r1);
//		Validator.Result result = validator.validate(r0, r1);
		boolean valid = result.isValid();
		List<Error> errors = result.getErrors();
		
		if (!valid) System.out.println(errors.get(0).getError());
		System.out.println(valid);
		
		assertTrue(valid);
		
		// errors
		
		
		result = validator.validate(topSchema.woString(3), r2);
		
		valid = result.isValid();
		assertFalse(valid);
		
		errors = result.getErrors();
		
		String mess = errors.stream().map(
				error -> {return error.getInstanceLocation() + ": " + error.getError();}
				).collect(Collectors.joining("\n"));
		
		assertEquals(("/Person/personal: Object does not have some of the required properties [[name]]\r\n"
				+ "/Person/skills: False schema always fails\r\n"
				+ "/Person/favorites: False schema always fails\r\n"
				+ "/Person/personal: False schema always fails"
				+ "").replace("\r", ""), mess);
		
	}
	
	@Test public void testSchemaBundle1() throws Exception {
		
		Node prototypes = NodeFactory
				.w_(IOUtils.toString(new FileReader("src\\test\\resources\\japath3\\schema\\prototypes-1.jsonc")));
		JsonSchemaProcessing js = new JsonSchemaProcessing().setModular(true).setOnlyTopModular(true);
		
		js.usePrototypeBundle(prototypes);
		
//		System.out.println(js.buildJsonTopSchema(prototypes).woString(3));
		
		Node instance = NodeFactory.w_(inst1);
		
		Option<List<Error>> errors = js.validate(instance, "Person");
		
		assertEquals("/personal/age: Value is [string] but should be [integer]\n"
				+ "/personal: False schema always fails", js.errorText(errors.get()));
		
		// Project
		
		instance = NodeFactory.w_(inst2);
		
		errors = js.validate(instance, "Project");
		
		assertTrue(errors.isEmpty());
		
		// resolve
		
		Node res = js.getResolvedPrototypeBundle();
		
		System.out.println(res.woString(3));
		Testing.assertEquals_(getClass(), "testSchemaBundle1-1", res.woString(3));
		
		// cyclic
		
		String proto = """
			{
			    "$defs": {
			        "A": {
				        "x": {"$ref": "#/$defs/B"}
			        },
			        "B": {
				        "y": {"$ref": "#/$defs/A"}
			        }
			    }
			}
				""";

		prototypes = NodeFactory.w_(proto);
		
		try {
			js = new JsonSchemaProcessing().setModular(true).setOnlyTopModular(true).usePrototypeBundle(prototypes);
			fail();
		} catch (Exception e) {
			// ok
		}
	}
	

	public static String inst1 = 
			"""
			{
				"personal": {
			   	"name": "Miller",
			      "age": ""
			    }
			}
			"""
			;
	public static String inst2 = 
			"""
				{
		        "name": "proj1",
		        "lead": 
					{
						"personal": {
					   	"name": "Miller",
					      "age": 13
					    }
					},
		        "optSkills": [
		            {
		                "topic": "python",
		                "level": 2
		            }
		        ]
				}
			"""
			;
	
	public static String r1 = """
{
    "Person": {
        "personal": {
            "name": "Miller",
            "age": 17
        },
        "skills": [
            {
                "topic": "java",
                "level": 1
            },
            {
                "topic": "python",
                "level": 2
            }
        ],
        "favorites": [
            "coen-brothers",
            "dylan"
        ]
    },
    "Project": {
        "name": "proj1",
        "lead": {
        "personal": {
            "name": "Miller",
            "age": 17
        },
        "skills": [
            {
                "topic": "java",
                "level": 1
            },
            {
                "topic": "python",
                "level": 2
            }
        ],
        "favorites": [
            "coen-brothers",
            "dylan"
        ]
        },
        "optSkills": [
            {
                "topic": "python",
                "level": 2
            }
        ]
    }
}			
			"""; 

	static String r2 = """
			{
    "Person": {
        "personal": {
            //"name": "Miller",
            "age": 17
        },
        "skills": [
            {
                "topic": "java",
                "level": 1
            },
            {
                "topic": "python",
                "level": 2
            }
        ],
        "favorites": [
            "coen-brothers",
            "dylan"
        ]
    },
    "Project": {
        "name": "proj1",
        "lead": {
        "personal": {
            "name": "Miller",
            "age": 17
        },
        "skills": [
            {
                "topic": "java",
                "level": 1
            },
            {
                "topic": "python",
                "level": 2
            }
        ],
        "favorites": [
            "coen-brothers",
            "dylan"
        ]
        },
        "optSkills": [
            {
                "topic": "python",
                "level": 2
            }
        ]
    }
			}			
			"""; 
	
}
