package japath3.wrapper;

import java.io.File;
import java.io.PrintWriter;
import java.time.Instant;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class JsoupTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void test() throws Exception { 
		
		File input = new File("C:\\Users\\andreas-fm\\Desktop\\temp\\bookmarks.html");
		Document doc = Jsoup.parse(input, "UTF-8", "http://example.com/");
		
		Elements links = doc.select("a[href]");
		
		for (Element elm : links) {
//			System.out.println(elm);
			String d = elm.attr("ADD_DATE");
//			System.out.println(d);
			Instant ofEpochSecond = Instant.ofEpochSecond(Long.parseLong(d));
//			System.out.println(ofEpochSecond);
			elm.append("  <span style=\"color:red\">__________________" + ofEpochSecond
					+ "</span>");
//			elm.append("   [" + ofEpochSecond
//					+ "]");
		}
		
//		System.out.println(doc.toString());
		
		PrintWriter pw = new PrintWriter("C:\\Users\\andreas-fm\\Desktop\\temp\\bookmarks_.html");
		pw.print(doc.toString());
		pw.close();
		
	}

}
