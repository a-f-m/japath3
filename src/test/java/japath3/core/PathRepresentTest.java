package japath3.core;

import static japath3.wrapper.NodeFactory.w_;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import japath3.util.Coding;
import japath3.util.Testing;
import japath3.wrapper.WGson;

public class PathRepresentTest {
	
	public static Coding pathReprCoding = new Coding('_').setAllowedCharsRegex("[a-zA-Z0-9_\\-]");

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test 
	public void testPathRepr() throws Exception {
		
		Node n = w_(
				"""						
				{
				   "text": "lolo",
				   "$names": [
					   {
					      "first": "john",
					      "last": "miller"
					   },
						{
							"first": "johnny",
							"last": "muller"
						}
				   ],
				   "age": 99
				}
				"""
				);
		PathRepresent pr = new PathRepresent("_raw").setFieldCoding(pathReprCoding);
		Node n_;
		Node n__;
		testToFrom(n, pr, "testPathRepr_1", true);
		
	}

	private void testToFrom(Node n, PathRepresent pr, String t, boolean checkEq) {
		
		Node n_ = pr.toFlatNode(n);

		System.out.println(n_.woString(3));
		Testing.assertEquals_(t, n_.woString(3));

		Node n__ = pr.toStructNode(n_);
		System.out.println(n__.woString(3));

		if (checkEq) Assert.assertEquals(n.woString(3), n__.woString(3));
	}
	
	@Test 
	public void testPathReprLeafArraysPlusRegex() throws Exception {
		
		Node n = w_(
				"""						
				{
				   "$names": [
					   {
					      "first": ["john", "james"],
					      "last.num_All~w\\\\ed":  ["miller", "muller", 88]
					   }
				   ]
				}
				""" 
				);
		PathRepresent pr = new PathRepresent().setLeafArray(true).setFieldCoding(pathReprCoding);
		testToFrom(n, pr, "testPathReprLeafArrays_1", true);
		
		// path repr
		
		Node n_ = pr.toFlatNode(n);
		
		System.out.println(n_);
		
		String s = "_24_names.0.last_2e_num_5f_All_7e_w_5c_ed";
		String r = "'$names.~*.l[a-x]st'.num'_All'~w'\\ed";
		
		String r_ = pr.encodePathRegex(r);
		System.out.println(r_);
		assertTrue(s.matches(r_));
		
	}

	@Test 
	public void testPathSchema() throws Exception {
		
		Node n = w_(
				"""						
				{
				   "$names": [
					   {
					      "first": ["john", "james"],
					      "last.num_All~w\\\\ed":  ["miller", "muller", 88]
					      //,
					      //"0": 88
					   }
				   ]
				}
				""",
				WGson.class
				);
		PathRepresent pr = new PathRepresent().setLeafArray(false).setFieldCoding(pathReprCoding);

		String actual = pr.toPathSchema(n).woString(3);
		System.out.println(actual);
		Testing.assertEquals_(getClass(), "path-schema-1", actual);
	}

	@Test 
	public void testSingleInt() throws Exception {
		
		Node n = w_(
				"""						
				{
				   "0": [
					   {
					      "first": ["john", "james"],
					      "10": 88
					   }
				   ]
				}
				"""
				);
		PathRepresent pr = new PathRepresent().setLeafArray(false).setFieldCoding(pathReprCoding);
		
		testToFrom(n, pr, "testSingleInt_1", true);
	}

	@Test 
	public void testPreserveTopProp() throws Exception {
		
		Node n = w_(
				"""						
				{
					"a": 22,
					"b": {"c": 8},
					"_id": "99"
				}
				"""
				);
				PathRepresent pr = new PathRepresent("_path").setPreserveTopPropRegex("_id")
						.setLeafArray(false)
						.setFieldCoding(pathReprCoding);
		
		testToFrom(n, pr, "testPreserveTopProp", true);
		
	}

	@Test 
	public void testSubscripts() throws Exception {
		
		PathRepresent pr = new PathRepresent().setLeafArray(false).setFieldCoding(pathReprCoding);

		assertTrue(pr.containsSubscripts("22.a.b"));
		assertTrue(pr.containsSubscripts("a.10.b"));
		assertTrue(pr.containsSubscripts("a.10"));
		assertFalse(pr.containsSubscripts("a.b.c"));
	}

}
