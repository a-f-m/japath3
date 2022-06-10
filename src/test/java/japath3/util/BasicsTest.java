package japath3.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static japath3.util.Basics.Switches.checkSwitches;
import static japath3.util.Basics.Switches.switchEnabled;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.vavr.control.Option;

public class BasicsTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {}

	@Test
	public void testEncode() { 
		
		String s1 = "\u274Cla la_lo%lo-%005f";
		System.out.println(s1);
		String s = Basics.encode_(s1);
		System.out.println(s);
		System.out.println(Basics.decode_(s));
		
		assertEquals(s1, Basics.decode_(Basics.encode_(s1)));

		s1 = "a_b__ QQQ WWW c§$%&";
		String es1 = Basics.encode_(s1);
		System.out.println(es1);
		assertEquals(s1, Basics.decode_(es1));
		
		s1 = "a b c";
		es1 = Basics.encode_(s1);
		System.out.println(es1);
		assertEquals(s1, Basics.decode_(es1));

		
	}

	@Test
	public void testPretty() {
		
		String s = "( aa,    b, ff(cc, d), g(e))";
		System.out.println(s);
		System.out.println(Basics.prettyNesting(s, ",", ""));
		System.out.println("---");
		s = "1 + (2 + 3) * (4 * (5 + 6))";
		System.out.println(s);
		System.out.println(Basics.prettyNesting(s, " ", ""));
		System.out.println("---");
		
		s = "and( "
				+ "	`request`. "
				+ "	`input`. "
				+ "	type(String),  "
				+ "	`selectedTopics`. "
				+ "	every( "
				+ "		`text`. "
				+ "		type(String)),  "
				+ "	`time`. "
				+ "	and( "
				+ "		`startDate`. "
				+ "		type(String),  "
				+ "		`endDate`. "
				+ "		type(String)),  "
				+ "	`programmefilter`. "
				+ "	every( "
				+ "		and( "
				+ "			`domain`. "
				+ "			type(String),  "
				+ "			`type`. "
				+ "			type(String),  "
				+ "			`programme`. "
				+ "			type(String))),  "
				+ "	`partner`. "
				+ "	`name`. "
				+ "	type(String),  "
				+ "	`number`. "
				+ "	type(String),  "
				+ "	`acronym`. "
				+ "	type(String),  "
				+ "	`sortBy`. "
				+ "	every( "
				+ "		type(String))) "
				+ "";

		System.out.println(Basics.prettyNesting(s, 1, ",", "\\.", "(type)"));
		System.out.println("---");
		
//		String[] split = s.split("\\(|\\)");
//		System.out.println(Basics.prettyNesting1(split, ",", "\\."));
//		System.out.println("---");
	}
	
	
	@Test
//	@Ignore
	public void testSwitches() throws Exception {
		
		String switchPatt = "a|b";
		assertEquals(Option.none(), checkSwitches("a,b", switchPatt));
		assertEquals(Option.none(), checkSwitches(" a , b ", switchPatt));
		
		
		assertTrue(switchEnabled(" a , b , c", "a"));
		assertTrue(switchEnabled(" a , b , c", "b"));
		assertTrue(switchEnabled(" a , b , c", "c"));
		
	}


	
	@Test
	@Ignore
	public void doHttpSock() throws Exception {
		
		@SuppressWarnings("resource")
		ServerSocket socket = new ServerSocket(8081);

		while (true) {
			Socket connection = socket.accept();

			InputStream is = connection.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			OutputStream out = new BufferedOutputStream(connection.getOutputStream());
			PrintStream pout = new PrintStream(out);
		
//			String s = IOUtils.toString(is, "utf-8");
//			System.out.println(s);
			
			while (true) {
				String ignore = in.readLine();
				System.out.println(ignore);
				if (ignore == null || ignore.length() == 0) break;
			}

			String response = "Hello, World!";
			String newLine = "\n";

			pout.print("HTTP/1.0 200 OK" + newLine
					+ "Content-Type: text/plain"
					+ newLine
//					+ "Content-length: "
//					+ response.length()
//					+ newLine
					+ newLine
					+ response
					);
			
			pout.close();
		}
	}
	
	@Test
//	@Ignore
	public void doPrettyNesting() throws Exception {
		
		
		String s = "custom((((fullContent:secur)^0.06124861) ((fullContent:research)^0.06124861) ((fullContent:secur)^0.06124861) ((fullContent:forc)^0.06124861) ((fullContent:secur)^0.06124861) ((fullContent:societi)^0.06124861) ((fullContent:secur)^0.06124861) ((fullContent:econom)^0.06124861)) (((fullContent:border)^0.06541875) ((fullContent:manag)^0.06541875)) (((fullContent:secur)^0.06832233) ((fullContent:emerg)^0.06832233) ((fullContent:forc)^0.06832233)) (((fullContent:organis)^0.064585656) ((fullContent:structur)^0.064585656) ((fullContent:cultur)^0.064585656) ((fullContent:public)^0.064585656) ((fullContent:user)^0.064585656)) (((fullContent:secur)^0.08486665) ((fullContent:manag)^0.08486665)) (((fullContent:crisi)^0.062369432) ((fullContent:manag)^0.062369432) ((fullContent:system)^0.062369432)) (((fullContent:secur)^0.06659906) ((fullContent:commun)^0.06659906)) (((fullContent:foresight)^0.1) ((fullContent:scenario)^0.1) ((fullContent:secur)^0.1) ((fullContent:evolv)^0.1) ((fullContent:concept)^0.1)) (((fullContent:secur)^0.09778378) ((fullContent:manag)^0.09778378) ((fullContent:practic)^0.09778378)) (((fullContent:border)^0.07721507) ((fullContent:secur)^0.07721507)) (((fullContent:design)^0.07404166) ((fullContent:plan)^0.07404166) ((fullContent:build)^0.07404166) ((fullContent:urban)^0.07404166) ((fullContent:area)^0.07404166)) (((fullContent:urban)^0.07070461) ((fullContent:secur)^0.07070461)) (((fullContent:urban)^0.08362174) ((fullContent:secur)^0.08362174) ((fullContent:solut)^0.08362174)) (((fullContent:surveil)^0.09548265) ((fullContent:border)^0.09548265) ((fullContent:secur)^0.09548265)) (((fullContent:secur)^0.07416575) ((fullContent:suppli)^0.07416575) ((fullContent:chain)^0.07416575) ((fullContent:data)^0.07416575) ((fullContent:secur)^0.07416575) ((fullContent:privaci)^0.07416575)) (((fullContent:emerg)^0.07869532) ((fullContent:disast)^0.07869532) ((fullContent:manag)^0.07869532)) ((fullContent:enppf)^1.0 (fit_number_std:enppf)^1.5))";

		System.out.println(Basics.prettyNesting(s , "", "fullContent"));
	}

}
