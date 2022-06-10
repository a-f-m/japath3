package japath3.util;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import japath3.core.JapathException;

/**
 * json order extension: enables insert-order and provides custom parsing for org.json.   
 *
 */
public class JoeUtil {

//	public static class Joe extends JSONObject {
//
//		private static Field dmap;
//		
//		static {
//			try {
//			dmap = JSONObject.class.getDeclaredField("map");
//			dmap.setAccessible(true);
//			
//		} catch (IllegalArgumentException | NoSuchFieldException | SecurityException e) {
//			throw new JapathException(e);
//		}
//			
//		}
//
//		public Joe() {
//
//			try {
////				boolean accessible = dmap.isAccessible();
//				dmap.set(this, new LinkedHashMap());
//			} catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
//				throw new JapathException(e);
//			}
//		}
//		
//		public Joe(String source) throws JSONException { super(source); }
//
//		public Joe(JSONTokener x) {
//			this();
//			if (x != null) parse(x);
//		}
//
//		// we copied fron org.json; here we have the chance to extend
//		public Joe parse(JSONTokener x) {
//
//			char c;
//			String key;
//
//			if (x.nextClean() != '{') {
//				throw x.syntaxError("A JSONObject text must begin with '{'");
//			}
//			for (;;) {
//				c = x.nextClean();
//				switch (c) {
//				case 0:
//					throw x.syntaxError("A JSONObject text must end with '}'");
//				case '}':
//					return this;
//				default:
//					x.back();
//					key = x.nextValue().toString();
//				}
//
//				// The key is followed by ':'.
//
//				c = x.nextClean();
//				if (c != ':') {
//					throw x.syntaxError("Expected a ':' after a key");
//				}
//
//				// Use syntaxError(..) to include error location
//
//				if (key != null) {
//					// Check if key exists
//					if (opt(key) != null) {
//						// key already exists
//						throw x.syntaxError("Duplicate key \"" + key + "\"");
//					}
//					// Only add value if non-null
//					Object value = x.nextValue();
//					if (value != null) {
//						put(key, value);
//					}
//				}
//
//				// Pairs are separated by ','.
//
//				switch (x.nextClean()) {
//				case ';':
//				case ',':
//					if (x.nextClean() == '}') {
//						return this;
//					}
//					x.back();
//					break;
//				case '}':
//					return this;
//				default:
//					throw x.syntaxError("Expected a ',' or '}'");
//				}
//			}
//		}
//	}

//	private static Field eof;
//
//	static class AdaptedTokener extends JSONTokener {
//		
//		
//
//		public AdaptedTokener(InputStream inputStream) {
//			super(inputStream);
//			eof();
//		}
//
//		public AdaptedTokener(String s) {
//			super(s);
//			eof();
//		}
//
//		private void eof() {
//			try {
//				eof = JSONTokener.class.getDeclaredField("eof");
//			} catch (NoSuchFieldException | SecurityException e) {
//				throw new JapathException(e);
//			}
//			eof.setAccessible(true);
//		}
//
//		// we copied fron org.json; here we have the chance to extend
//		@Override
//		public Object nextValue() throws JSONException {
//
//			char c = this.nextClean();
//			String string;
//
//			switch (c) {
//			case '"':
//			case '\'':
//				return this.nextString(c);
//			case '{':
//				this.back();
//				return new Joe(this);
//			case '[':
//				this.back();
//				return new JSONArray(this);
//			}
//
//			/*
//			 * Handle unquoted text. This could be the values true, false, or null,
//			 * or it can be a number. An implementation (such as this one) is
//			 * allowed to also accept non-standard forms.
//			 *
//			 * Accumulate characters until we reach the end of the text or a
//			 * formatting character.
//			 */
//
//			StringBuilder sb = new StringBuilder();
//			while (c >= ' ' && ",:]}/\\\"[{;=#".indexOf(c) < 0) {
//				sb.append(c);
//				c = this.next();
//			}
//			try {
//				if (!eof.getBoolean(this)) {
//					this.back();
//				}
//			} catch (IllegalArgumentException | IllegalAccessException e) {
//				throw new JapathException(e);
////				e.printStackTrace();
//			}
//
//			string = sb.toString().trim();
//			if ("".equals(string)) {
//				throw this.syntaxError("Missing value");
//			}
//			return JSONObject.stringToValue(string);
//		}
//		
//		@Override public char nextClean() throws JSONException { 
//	        for (;;) {
//	            char c = this.next();
//	            if (c == '/') {
//	            	c = this.next();
//	            	if (c == '/') {
//	            		while ((c = this.next()) != '\n');
//	            	}
//					}
//	            if (c == 0 || c > ' ') {
//	                return c;
//	            }
//	        }
//		}
//
//	}

	private static String commentRegex = "(?m)([^:]|^)//.*?$";

	public static JSONObject createJoe() {
//		return new Joe();
		return new JSONObject();
	}
	
	public static JSONObject createJoe(String txt) {
		return new JSONObject(txt.replaceAll(commentRegex, ""));
	}
	
	public static JSONObject createJoe(InputStream inputStream) {
//		return new Joe(new AdaptedTokener(inputStream));
		try {
			return createJoe(IOUtils.toString(inputStream, "utf-8"));
		} catch (IOException e) {
			throw new JapathException(e);
		}
//		return new JSONObject(new JSONTokener(inputStream));
	}
	
//	 extra case for json arrays, only used for input streams
	public static JSONArray createJoeArray(InputStream inputStream) {

		try {
			return new JSONArray(IOUtils.toString(inputStream, "utf-8").replaceAll(commentRegex, ""));
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}
	
	public static Object copy(Object o) {
		
		return o instanceof JSONObject ? new JSONObject(o.toString())
				: o instanceof JSONArray ? new JSONArray(o.toString()) : o;
	}

	public static String prettyString(Object o, int indent) {

		return o instanceof JSONObject ? ((JSONObject) o).toString(indent)
				: o instanceof JSONArray ? ((JSONArray) o).toString(indent) : o.toString();
	}
	
	public static void main(String[] args) {
		String s = "\"lala\": \"http://\" // lolo\n"
				+ " xxx\n"
				+ "// yyy\n"
				+ "//\n"
				+ "//	\"solr\": \"http://tm-solr:8983/solr/labeco\",\n"
				+ "uuu\n"
				+ "{\r\n"
				+ "	\"solr\": \"http://localhost:8984/solr/labeco\",\r\n"
				+ "//	\"solr\": \"http://tm-solr:8983/solr/labeco\",\r\n"
				+ "	\"rdfStore\": \"http://localhost:3030/labeco\"\r\n"
				+ "}";
		
		System.out.println(s);
		System.out.println("---");
//		System.out.println(s.replaceAll("[^:]//[^\"]*?\n", "\n"));
		System.out.println(s.replaceAll("(?m)([^:]|^)//.*?$", ""));
	}
}
