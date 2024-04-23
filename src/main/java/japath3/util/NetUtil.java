package japath3.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.json.JSONObject;

import io.vavr.collection.List;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.wrapper.NodeFactory;
import okhttp3.Cache;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NetUtil {
	
	public static record Param(String name, String value) {
		private String urlForm() {
			try {
				return name == null ? "" : name + "=" + URLEncoder.encode(value, "utf-8");
			} catch (UnsupportedEncodingException e) {
				throw new JapathException(e);
			}
		}
		public static String urlFormList(Param[] params, boolean append) {

			return List.of(params).map(x -> {
				return x.urlForm();
			}).mkString((append ? "&" : ""), "&", "");
		}
		public static Param p_() {
			return new Param(null, null);
		}
		public static Param p_(String name, Object value) {
			return new Param(name, value.toString());
		}
	}
	
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
		public okhttp3.Response origResponse;
		public Request origRequest;
		
		public Response(Request origRequest, okhttp3.Response origResponse, String entity, int status) {
			this.entity = entity;
			this.status = status;
			this.origResponse = origResponse;
			this.origRequest = origRequest;
		}
		public Response(String entity, int status) {
			this.entity = entity;
			this.status = status;
		}
		public Node entityAsJson(Class<?> wrapperClass) {
			return NodeFactory.w_(entity, wrapperClass);
		}
		public Node entityAsJson() {
			return NodeFactory.w_(entity);
		}
	}

	public static Response httpPostJson(OkHttpClient client, String url, JSONObject jo) {
		
		return httpPost(client, url, jo.toString()); 		
	}
	
	public static Response httpPost(OkHttpClient client, String url, String s) {

		Request request = buildPostRequest(url, s);
		try (okhttp3.Response response = client.newCall(request).execute()) {
			return new Response(request, response, getResponseString(response), response.code());
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}

	public static Request buildPostRequest(String url, String s) {
		RequestBody body = RequestBody.create(s, JSON);
		Request request = new Request.Builder().url(url).post(body).build();
		return request;
	}	

	public static String getResponseString(okhttp3.Response resp) {
		try {
//			return IOUtils.toString(resp.body().byteStream(), "utf-8");
			return new String(resp.body().bytes());
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}
	
	public static Response httpGet(OkHttpClient client, String url, Param... params) {

		Request request = new Request.Builder().url( //
				url + (params.length == 0 ? ""
						: (url.contains("?") ? "" : "?") + Param.urlFormList(params, !url.endsWith("&"))))
				.build();
		try (okhttp3.Response response = client.newCall(request).execute()) {
			return new Response(request, response, getResponseString(response), response.code());
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}

	/** TODO call it from 'httpGet'	 */
	public static okhttp3.Response okHttpGet(OkHttpClient client, String url, Param... params) {
		
		Request request = new Request.Builder().url( //
				url + (params.length == 0 ? ""
						: (url.contains("?") ? "" : "?") + Param.urlFormList(params, !url.endsWith("&"))))
				.build();
		
//		Request request = new Request.Builder().url("http://localhost:8985/solr/test-0/stream?&expr=search%28%0A%09test-0%2C%0A%09q%3D%22id%3A*%22%0A%29%0A").build();
		
//		Request request = new Request.Builder().url("http://localhost:8985/solr/test-0/stream?&expr=search%28%0A%09test-0%2C%0A%09q%3D%22id%3A*%22%0A%29%0A")
//				.build();
		try {
			return client.newCall(request).execute();
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}
	
	public static InputStream getStreamFromUrlOrFile(String spec) {
		
		InputStream is;
		try {
			URL url = new URL(spec);
			
			try {
				is = url.openStream();
			} catch (IOException e) {
				throw new JapathException(e);
			}
			
		} catch (MalformedURLException e1) {
			try {
				is = new FileInputStream(spec);
			} catch (FileNotFoundException e) {
				throw new JapathException(e);
			}
		}
		return is;
	}
	
	public static void main(String[] args) {
		
        @SuppressWarnings("unused")
		OkHttpClient client = 
        		 new OkHttpClient.Builder()
        		 
     		      .readTimeout(5, TimeUnit.MINUTES)
     		      .cache(new Cache(new File(".../http-cache"), 50L * 1024L * 1024L))
     		      
     		      .addInterceptor(
     		    		  chain -> {
     		    			 okhttp3.Response response = chain.proceed(chain.request());
     		    			 System.out.println(response.headers().toString());
     		    			okhttp3.Response resp_ = response
     		    		                .newBuilder()
     		    		                .removeHeader("cache-control") 
     		    		                .removeHeader("X-Content-Type-Options") 
     		    		                .removeHeader("X-XSS-Protection") 
     		    		                .removeHeader("X-Frame-Options") 
     		    		                .header("cache-control", "public, max-age=1000") 
     		    		                .removeHeader("pragma")  
     		    		                .removeHeader("expires") 
     		    		                .removeHeader("x-cache") 
     		    		                .build();
      		    			 System.out.println("---");
      		    			 System.out.println(resp_.headers().toString());

							return resp_;
     		    		  }
     		    		  )
     		      .build();

		
		Param[] ppp = new Param[] {new Param("x", "yy << c"), new Param("x", "yy << c")};
		
		System.out.println(Param.urlFormList(ppp, false));
		
		httpGet(new OkHttpClient(), "http://a.b/c", Param.p_("a", "b"));
		
	}
}
