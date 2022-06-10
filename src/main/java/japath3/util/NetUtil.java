package japath3.util;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import japath3.core.JapathException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NetUtil {
	
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	
	public static class StandardFormatter extends SimpleFormatter {

		public StandardFormatter() {
			super();
		}

		@Override
		public String format(LogRecord record) {

			String mess =
				new SimpleDateFormat().format(new Date(record.getMillis())) 
					+ " " + record.getLevel().getName()
					+ " " + record.getSourceClassName()
					+ "."
					+ record.getSourceMethodName()
					+ ":   "
					+ record.getMessage()
					+ "\n";
			return mess;
		}

		@SuppressWarnings("unused")
		private static String trim(String s, int k) {

			s = s.length() > k ? s.substring(0, k - 2) + ".." : s;
			String x = String.format("%1$-" + k + "s", s);
			return x;
		}
		
		public static void initDefaultLogging() {
			for (Handler han : Logger.getLogger("").getHandlers()) {
				han.setFormatter(new StandardFormatter());
			}

		}

	}


	public static class Response {
		
		public String entity;
		public int status;
		public Response(String entity, int status) {
			super();
			this.entity = entity;
			this.status = status;
		}
		
	}

	public static Response httpPostJson(OkHttpClient client, String url, JSONObject jo) {
		
		return httpPost(client, url, jo.toString()); 		
	}
	
	public static Response httpPost(OkHttpClient client, String url, String s) {

		RequestBody body = RequestBody.create(s, JSON);
		Request request = new Request.Builder().url(url).post(body).build();
		try (okhttp3.Response response = client.newCall(request).execute()) {
			return new Response(getResponseString(response), response.code());
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}	

	public static String getResponseString(okhttp3.Response resp) {
		try {
			return IOUtils.toString(resp.body().byteStream(), "utf-8");
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}
	
	public static Response httpGet(OkHttpClient client, String url) {

		Request request = new Request.Builder().url(url).build();
		try (okhttp3.Response response = client.newCall(request).execute()) {
			return new Response(getResponseString(response), response.code());
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}


}
