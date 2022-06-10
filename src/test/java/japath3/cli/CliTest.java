package japath3.cli;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import japath3.util.Basics;

public class CliTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test 
//	@Ignore 
	public void test() {

		String args = "" 
//				+ " --op schemaGen "
				+ " --op select "
//				+ " --type xml "
				+ "--text {x:{\\ y:1,\\ z:'hi'}} "
//				+ "--text <a>lala</a> "
//				+ "-constraints and(x.and(y.type(Number),z.type(String))) "
				+ "-apathExpr x$v.y "
//				+ "-apathExpr x.y "
//				+ "-apathExpr x.* "
//				+ "-apathExpr a.text()$ "
//				+ "-apathExpr self "
				+ "-var v "
//				+ "-var  "
				+ "--pretty "
				+ "--salient "
//				+ "--service 9090 "
//				+ "--asArray "
//				+ "--schemaGenSwitches opt,selectorRestriction"
				;
		
		// bash: java -Dfile.encoding=UTF-8 -jar japath-cli.jar --op select --text "{x:{ y:1}}" -s "and(\`x\`.and(\`y\`.type(Number)))" -a "x$.y" -v x
		
		Commands.main(Basics.makeArgs(args));

	}


}
