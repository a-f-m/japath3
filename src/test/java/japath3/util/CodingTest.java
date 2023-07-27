package japath3.util;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.florianingerl.util.regex.Pattern;

public class CodingTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void test1() { 
		
		try {
			URLEncoder.encode("", "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		Coding coding = new Coding('_').setSpecialChars(Coding.UrlSpecial);
		
		String s1 = "xü\u274Cla la_lo%lo-%005f";
		System.out.println(s1);
		String s = coding.encode(s1);
		System.out.println(s);
		System.out.println(coding.decode(s));
		
		assertEquals(s1, coding.decode(coding.encode(s1)));
		
		s1 = "a_b__ QQQ WWW c§$%&";
		String es1 = coding.encode(s1);
		System.out.println(es1);
		assertEquals(s1, coding.decode(es1));
		
		s1 = "a b c";
		es1 = coding.encode(s1);
		System.out.println(es1);
		assertEquals(s1, coding.decode(es1));

		s1 = "_a_b_c_";
		es1 = coding.encode(s1);
		System.out.println(es1);
		assertEquals(s1, coding.decode(es1));

		s1 = "_14_b_c_";
		es1 = coding.encode(s1);
		System.out.println(es1);
		assertEquals(s1, coding.decode(es1));
		
		s1 = "person_children";
		es1 = coding.encode(s1);
		System.out.println(es1);
		assertEquals(s1, coding.decode(es1));
		
		coding = new Coding('-').setSpecialChars(Coding.FhirSpecial + '.');
		
		s1 = "http://hl7.org/fhir/ValueSet/my-valueset|0.8";
		System.out.println(s1);
		es1 = coding.encode(s1);
		System.out.println(es1);
		assertEquals(s1, coding.decode(es1));
		
		coding = new Coding('_').setAllowedCharsRegex(Coding.IdRegex);
		
		s1 = "http://hl7.org/fhir/ValueSet/my-valueset|0.8";
		System.out.println(s1);
		es1 = coding.encode(s1);
		System.out.println(es1);
		assertEquals(s1, coding.decode(es1));
		
		coding = new Coding('_').setAllowedCharsRegex("x");

		s1 = "10";
		System.out.println(s1);
		es1 = coding.encode(s1);
		System.out.println(es1);
		assertEquals(s1, coding.decode(es1));
		
		coding = new Coding('_').setAllowedCharsRegex(Coding.IdRegex);
		assertEquals("10", coding.decode("_31__30_"));
		
		
	}
}
