package japath3.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.florianingerl.util.regex.Pattern;

public class RegexTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void testExtract() { 
		
//		String[] def = 

		String r = "(?:<(.+?)>)?\\s*([^<>]+)";
		
		String[] x = Regex.multiExtract(" ", r);
		assertEquals("[, null, ]", Arrays.asList(x).toString());

		x = Regex.multiExtract("lala", r);
		assertEquals("[lala, null, lala]", Arrays.asList(x).toString());
		
		x = Regex.multiExtract("<rel>", r);
		assertNull(x);
		
		x = Regex.multiExtract("<rel> lala", r);
		assertEquals("[<rel> lala, rel, lala]", Arrays.asList(x).toString());
		
		x = Regex.multiExtract(" 12.44. xxxx ", "((\\d|\\.|\\s)*)(.+)");
		assertEquals("[12.44. xxxx, 12.44., , xxxx]", Arrays.asList(x).toString());
		
		x = Regex.multiExtract("12.44. xxxx ", "((\\d|\\.|\\s)*)(.*)");
		assertEquals("[12.44. xxxx, 12.44., , xxxx]", Arrays.asList(x).toString());
		
		String y = Regex.extract("12.44. xxxx ", "(?:(?:\\d|\\.|\\s)*)(.*)", null);
		assertEquals("[xxxx]", Arrays.asList(y).toString());
		
		x = Regex.multiExtract("1 xxxx", "((\\d|\\.|\\s)*)(.*)");
		assertEquals("[1 xxxx, 1, , xxxx]", Arrays.asList(x).toString());
		
		
	}
	
	@Test public void testRegexError() {
		
		//This is the regex for a comma-separated list of a or b 
		String regex = "\\s*(a|b)\\s*(\\,\\s*(a|b))*\\s*";
		
		String input = "a,x,b";
		
		boolean b = input.matches(regex);
		
		assertFalse(b);
		assertEquals("a, >>> unexpected input >>> x,b", Regex.getErrorString(Pattern.compile(regex), input));
	}

}
