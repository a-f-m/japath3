package japath3.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

import org.apache.commons.io.IOUtils;

import japath3.core.JapathException;

public class Testing {
	
	public static String dir = "src/test/resources/_expected";

	// !!!!!!!!!!!!!!!!!!!!!!
	public static boolean overwrite;
//	public static boolean overwrite = true;
	// !!!!!!!!!!!!!!!!!!!!!!
	
	public static void assertEquals_(String expectedFile, String actual, boolean overwriteOnlyThisboolean ) {
		assertEquals_("", expectedFile, actual, overwriteOnlyThisboolean);
	}
	
	public static void assertEquals_(String expectedFile, String actual) {
		assertEquals_("", expectedFile, actual, false);
	}

	public static void assertEquals_(String projectDir /* with trailing '/' */ , String expectedFile, String actual, boolean overwriteOnlyThis) { 
		
		try {
			String path = projectDir + dir + "/" + expectedFile + ".expected";
			File f = new File(path);
			if (!f.exists()) {
				new File(projectDir + dir).mkdirs();
				f.createNewFile();
			}
			if (overwrite || overwriteOnlyThis) {
				OutputStreamWriter fw1 = new OutputStreamWriter(new FileOutputStream(f), "utf-8");
//				FileWriter fw = new FileWriter(f);
				fw1.write(actual);
				fw1.close();
				System.out.println("!!!!!!!!!!!!!!!!!!!!!! overwrite !!!!!!!!!!!!!!!!!!!!!!");
			}
			String expected = IOUtils.toString(new FileInputStream(f), "utf-8");
			assertEquals(expected, actual);
		} catch (IOException e) {
			throw new JapathException(e);
		}
	 }
}
