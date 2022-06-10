package japath3.wrapper;

import java.io.StringReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

public class WGsonTest {

	@BeforeClass public static void setUpBeforeClass() throws Exception {}

	@AfterClass public static void tearDownAfterClass() throws Exception {}

	@Test public void test() throws Exception {

		StringReader sr = new StringReader("[{'a':1, 'b': {'a':1}},{'a':2, 'b':['x', 'y']}]");
		// StringReader sr = new StringReader("[\"a\",\"b\"]");

		try (JsonReader jsonReader = new JsonReader(sr)) {
			jsonReader.beginArray(); // start of json array
			int numberOfRecords = 0;
			while (jsonReader.hasNext()) { // next json array element

				Gson gson = new GsonBuilder().create();
				Object fromJson = gson.fromJson(jsonReader, JsonObject.class);

				System.out.println(fromJson);
				// Document document = gson.fromJson(jsonReader, Document.class);
				// do something real
				// System.out.println(document);
				numberOfRecords++;
			}
			jsonReader.endArray();
			System.out.println("Total Records Found : " + numberOfRecords);
		}
	}

}
