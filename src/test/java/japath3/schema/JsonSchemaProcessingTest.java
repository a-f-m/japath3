package japath3.schema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
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
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.schema.JsonSchemaProcessing.PropertyAnnotation;
import japath3.util.Testing;
import japath3.wrapper.NodeFactory;
import japath3.wrapper.WGson;

public class JsonSchemaProcessingTest {
	
	static Class h;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
		h = NodeFactory.getDefaultWrapperClass();
		NodeFactory.setDefaultWrapperClass(WGson.class);
		
		Testing.setwStackTrace(true);
		Testing.setPrintActual(true);
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
		NodeFactory.setDefaultWrapperClass(h);
		
		Testing.setwStackTrace(false);
		Testing.setPrintActual(false);
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
		
				Node jschema = new JsonSchemaProcessing().setOpt(false)
						.setModular(true)
						.setComplete(false)
						.setOnlyTopModular(false)
						.buildJsonTopSchema(jo);
				jschema.remove("$id");
		System.out.println(jschema.woString(3));
		
		Testing.assertEquals_("test1", jschema.woString(3));
		
	}
	
	@Test public void testSchemaBundle() throws Exception {
		
		Node bundle = NodeFactory
				.w_(IOUtils.toString(new FileReader("src\\test\\resources\\japath3\\schema\\prototypes-1.jsonc")));
		
//		System.out.println(bundle.woString(3));
		
		JsonSchemaProcessing js = new JsonSchemaProcessing().setModular(true).setComplete(false).setOnlyTopModular(false);
		
		Node topSchema = js.buildJsonTopSchema(bundle);
		System.out.println(topSchema.woString(3));
		
		Testing.assertEquals_("testSchemaBundle-1", topSchema.woString(3));
		
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
		JsonSchemaProcessing js = new JsonSchemaProcessing().setModular(true).setComplete(false).setOnlyTopModular(true);
		
		js.usePrototypeBundle(prototypes);
		
//		System.out.println(js.buildJsonTopSchema(prototypes).woString(3));
		
		Node instance = NodeFactory.w_(inst1);
		
		Option<List<Error>> errors = js.validate(instance, "Person");
		
		assertEquals("/personal/age: Value is [string] but should be [number]\n"
				+ "/personal: False schema always fails", JsonSchemaProcessing.errorText(errors.get()));
		
		// Project
		
		instance = NodeFactory.w_(inst2);
		
		errors = js.validate(instance, "Project");
		
		assertTrue(errors.isEmpty());
		
		// resolve
		
		Node res = js.getResolvedPrototypeBundle();
		
		System.out.println(res.woString(3));
		Testing.assertEquals_("testSchemaBundle1-1", res.woString(3));
		
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
	
	@Test public void testProtoAnnoOpt() throws Exception {
		
		assertEquals("PropertyAnnotation[opt_=false, minmax=null]", PropertyAnnotation.eval("lala | +", false).toString());
		assertEquals("PropertyAnnotation[opt_=true, minmax=null]", PropertyAnnotation.eval("lala|-", false).toString());
		assertNull(PropertyAnnotation.eval("lala", false));
		
		Node jo = NodeFactory.w_("""
				{
					"$defs": {
					    "Person": {
						    "age|+": 9,
						    "name|-": "Miller",
						    "post": {}
					    }
				    }
				}				
				""");
		
		JsonSchemaProcessing jsp = new JsonSchemaProcessing().usePrototypeBundle(jo);
		Testing.assertEquals_("opt-anno-1", jsp.getSchemaBundle().woString(3));
		
		jsp = new JsonSchemaProcessing().setOpt(true).usePrototypeBundle(jo);
		Testing.assertEquals_("opt-anno-2", jsp.getSchemaBundle().woString(3));

		Testing.assertEquals_("opt-anno-exas-1", jsp.getResolvedPrototypeBundle().woString(3));
		
//		System.out.println(PropertyAnnotation.removeAnnotation(jo.toString()));
	}
	
	@Test public void testProtoAnnoCard() throws Exception {
		
		try {
			assertNull(PropertyAnnotation.eval("-|lala", false));
			fail();
		} catch (JapathException e) {
			System.out.println(e.toString());
		}
		try {
			assertNull(PropertyAnnotation.eval("lala|lsls", false));
			fail();
		} catch (JapathException e) {
			System.out.println(e.toString());
		}
		try {
			assertNull(PropertyAnnotation.eval("lala|1...1", false));
			fail();
		} catch (JapathException e) {
			System.out.println(e.toString());
		}
		
		
		assertEquals("PropertyAnnotation[opt_=true, minmax=null]",
				PropertyAnnotation.eval("lala | -", false).toString());
		assertEquals("PropertyAnnotation[opt_=false, minmax=MinMax[min=0, max=*, allowEmpty=false]]",
				PropertyAnnotation.eval("lala | 0..*", false).toString());
		assertEquals("PropertyAnnotation[opt_=false, minmax=MinMax[min=9, max=18, allowEmpty=false]]",
				PropertyAnnotation.eval("lala | 9..18 ", false).toString());
		
		Node jo = NodeFactory.w_("""
				{
					"$defs": {
					    "Person": {
						    "meals|-|2..2": [
						    	"fish", "meat"
						    ],
						    "fav": [
						    	"dylan"
						    ]
					    }
				    }
				}				
				""");

		JsonSchemaProcessing jsp = new JsonSchemaProcessing().usePrototypeBundle(jo);
		Testing.assertEquals_("anno-1", jsp.getSchemaBundle().woString(3));
		
		Testing.assertEquals_("exas-1", jsp.getResolvedPrototypeBundle().woString(3));
		
		Testing.assertEquals_("vali-1", JsonSchemaProcessing.errorText(jsp.validate(NodeFactory.w_("""
				{
					"meals": ["a"],
					"fav": [
					  	"dylan"
					]
				}
				"""), "Person").get()).toString());
		
		jo = NodeFactory.w_("""
				{
					"$defs": {
					    "Person": {
						    "meals|-|?2..2": [
						    	"fish", "meat"
						    ]
					    }
				    }
				}				
				""");
		
		jsp = new JsonSchemaProcessing().usePrototypeBundle(jo);
		Testing.assertEquals_("anno-2", jsp.getSchemaBundle().woString(3));
		Testing.assertEquals_("vali-2", JsonSchemaProcessing.errorText(jsp.validate(NodeFactory.w_("""
				{
					"meals": ["a"]
				}
				"""), "Person").get()).toString());
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
