package japath3.util;

import static io.vavr.control.Option.none;
import static org.apache.commons.lang3.StringUtils.repeat;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.florianingerl.util.regex.Pattern;

import io.vavr.collection.List;
import io.vavr.control.Option;
import japath3.core.JapathException;


public class Basics {
	
	
	public static class Ref<T> {

		public T r;

		public Ref() {}

		public Ref(T value) { this.r = value; }

		public static <T> Ref<T> of(T t) { return new Ref<T>(t); }
		public static <T> Ref<T> of() { return new Ref<T>(); }

		@Override public String toString() { return "^" + (r == null ? "null" : r.toString()); }
	}
	
	public static record NestingSpec(int indent, String regexDelims, String regexSkipWSAfter, String noBreakToken) {}
	
	public static <T> T option(T o, T defaultVal) { return o == null ? defaultVal : o; }

	public static <T> Stream<T> stream(Iterable<T> it) { return StreamSupport.stream(it.spliterator(), false); }

	public static <T> Stream<T> stream(Iterator<T> itr) { return stream(it(itr)); }
	
	// for ambiguities
	public static <T> Stream<T> streamIt(Iterator<T> itr) { return stream(itr); }
	
	public static String decomment(String text) { return text.replaceAll("(?m)^(\\#|//).*", " "); }
	
	public static <T> Iterable<T> it(Iterator<T> it) {
		return new Iterable<T>() {
			@Override public Iterator<T> iterator() { return it; }
		};
	}
	
	public static <T> Iterable<T> action(Runnable action) {

		return new Iterable<T>() {
			@Override public Iterator<T> iterator() {
				return new Iterator<T>() {
					@Override public boolean hasNext() {
						action.run();
						return false;
					}
					@Override public T next() { throw new UnsupportedOperationException(); };
				};
			}
		};
	}

	// from http://stackoverflow.com/users/57695/peter-lawrey
	public static <K, V> Map<K, V> createLRUMap(final int maxEntries) {
		return new LinkedHashMap<K, V>(maxEntries * 10 / 7, 0.7f, true) {
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > maxEntries;
			}
		};
	}

	public static boolean contains(String tokens, String token) {
		return ("|" + tokens + "|").contains("|" + token + "|");
	}	
	
	public static <T> T whenNull(T t, T whenNull) {
		return t == null ? whenNull : t;
	}

	public static <T> T checkEqual(T curr, T last) {
		if (last != null) {
			if (!last.equals(curr)) return null;
		}
		return curr;
	}

	/**
	 * this extra non-standard encoding is needed to avoid ' '->'+' (at URL encoding). Used in situation where 
	 * '+' leads to errors, e.g. jena fuseki.
	 * @param s
	 * @return
	 */
	public static String encode_(String s) {
		
		String enc = "";
		for (int i = 0; i < s.length(); i++) {
			Character c = s.charAt(i);
			enc += (c == ' ' ? "__" : (nonSpecialChar(c) ? c : '_' + String.format("%04x", (int) c) + '_'));
		}
		return enc;
	}

	private static boolean nonSpecialChar(Character c) {
		return c == '.' || c == '-' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9');
	}
	
	public static String decode_(String s) {
		
		String dec = "";
		for (int i = 0; i < s.length(); i++) {
			Character c = s.charAt(i);
			if (nonSpecialChar(c)) {
				dec += c;
			} else if (c == '_' && i + 1 < s.length() && s.charAt(i + 1) == '_') {
				dec += ' ';
				i++;
			} else { // must be special
				dec += Character.valueOf((char) Integer.parseInt(s.substring(i + 1, i + 5), 16));
				i = i + 5;
			}
		}
		return dec;
	}
	
	public static String prettyNesting(String s, String regexDelims, String regexSkipWSAfter) {
		return prettyNesting(s, 1, regexDelims, regexSkipWSAfter, "");
	}
	
	public static String prettyNesting(String s, NestingSpec spec) {
		
		// TODO ugly, but dev time constraints
		s = s.replace("..", "\0");

		String ret = "";
		int pCnt = 0;
		boolean skipWS = false;
		for (int i = 0; i < s.length(); i++) {
			
			char c = s.charAt(i);
			String cs = String.valueOf(c);
			if (skipWS) {
				if (cs.matches("\\s")) continue;
				else skipWS = false;
			}
			boolean matchSkip = cs.matches(spec.regexSkipWSAfter);
			if (matchSkip) {
				skipWS = true;
//				pCnt++;
			}
			if (c == '(' || c == '{') {
				pCnt++;
				ret += c;
				boolean br = spec.noBreakToken.equals("") || !ret.matches("(?s).*" + spec.noBreakToken + "\\s*\\(");
				if (br) ret += pad(spec.indent, pCnt);
				skipWS = true;
			} else if (c == ')' || c == '}') {
				pCnt--;
				ret += c;
			} else if (String.valueOf(c).matches(spec.regexDelims)) {
				ret += c;
				ret += pad(spec.indent, pCnt);
				skipWS = true;
			} else {
				ret += c;				
			}
			if (matchSkip) {
//				pCnt--;
			}
		}
		return ret.replace("\0", "..");
	}
	
	public static String prettyNesting(String s, int indent, String regexDelims, String regexSkipWSAfter, String noBreakToken) {
		
		return prettyNesting(s, new NestingSpec(indent, regexDelims, regexSkipWSAfter, noBreakToken));
		
	}

	private static String pad(int indent, int level) {
		return indent == 0 ? "" : "\n" + repeat(repeat("\t", indent), level);
	}
	
	public static String prettyNesting_1(String s) {
		
		String tab = "\u2192";
//		String tab = "\\_";
//		String tab = "\t";
		
		String ret = "";
		int level = 0;
		for (int i = 0; i < s.length(); i++) {
			String c = String.valueOf(s.charAt(i));
			if (c.equals("(") || c.equals("{")) {
				
				level++;
				ret += c + '\n' + repeat(tab, level);
				
			}
			else if (c.equals(")") || c.equals("}")) {
				
				level--;
				
				ret += '\n' + repeat(tab, level) + c + '\n' + repeat(tab, level) ;
				
			} else {
				ret += c;
			}
		}
		
		String tab_ = "\\u2192";
		return ret.replaceAll("\\n+", "\n").replaceAll("\\n" + tab_ + "*\\n", "\n").replaceAll("(" + tab_ + ")\s+", "$1");
		//		return ret.replaceAll("\\n+", "\n").replaceAll("\\n\\\\_*\\n", "\n").replaceAll("(\\\\_)\s+", "$1");
//		return ret.replaceAll("\\n+", "\n").replaceAll("\\n\\t*\\n", "\n").replaceAll("(\\t)\s+\\(", "$1(");
		
	}
			

	public static <T> T c_(T t, Consumer<T> c) {
		c.accept(t);
		return t;
	}
	
	public static <T> T[] prepend(T x, T[] a, Class<T> clazz) {
		T[] ret = (T[]) Array.newInstance(clazz, a.length + 1);
		System.arraycopy(a, 0, ret, 1, a.length);
		ret[0] = x; 
		return ret;
	}
	
	public static Object[] setAt(int idx, Object x, Object[] a) {
		return setAt(idx, x, a, Object.class);
	}
	
	public static <T> T[] setAt(int idx, T x, T[] a, Class<T> clazz) {
		T[] ret;
		if (idx >= a.length) {
			ret = (T[]) Array.newInstance(clazz, idx + 1);
			System.arraycopy(a, 0, ret, 0, a.length);
		} else {
			ret = a;
		}
		ret[idx] = x; 
		return ret;
	}
	
	public static Object[] setIfAbsent(int idx, Object x, Object[] a) {
		return setIfAbsent(idx, x, a, Object.class);
	}
	
	public static <T> T[] setIfAbsent(int idx, T x, T[] a, Class<T> clazz) {
		
		if (idx >= a.length) {
			return setAt(idx, x, a, clazz);
		} else {
			return a;
		}
	}
	
	public static <K, V> io.vavr.collection.Map<K, List<V>> putExtend(io.vavr.collection.Map<K, List<V>> map, K key, V value) {
		
		return map.put(key, List.of(value), (x, y) -> {
			return x.append(y.head());
		});
	}
	
	public static <T> T getAt(int idx, T[] a, T defaultValue) {
		return idx >= a.length ? defaultValue : a[idx];
	}

	//!!! alias test
//	static class Xxx<T extends List<Tuple2<String, String>>> {
//		
//		public T f(T t) {
//			return (T) t.append(Tuple.of("la", "lo"));
//		}
//	}
//	static class Yyy<Ty extends Tuple2<Integer, Integer>> {
//		
//		public Ty f(Ty ty) {
//			return (Ty) Tuple.of(ty._1 + 1, ty._2 + 1);
//		}
//	}
//	
//	static class Zzz<Tz extends Yyy> {
//		
//		
//		
//		List<Tz> t = (List<Tz>)(List<?>) List.of(Tuple.of("1", "2"));
//		
//		public Tz g(Tz t_) {
//			return t_;
//			
//		}
//		
//	}
	
	public static <T> T align(Object o) {
		return (T) o;
	}
	
	public static Object checkType(Object o, Class<?>... classes) {
		boolean b = false;
		for (Class<?> clazz : classes) b = b || clazz.isInstance(o); 
		if (!b) throw new JapathException(o + " is not an instance of " + Arrays.asList(classes));
		return o;
	}
	
	public static String[] makeArgs(String args) {
		
		String[] split = args.split("(?<!\\\\) ");
		for (int i = 0; i < split.length; i++) split[i] = split[i].replace("\\ ", " ");
		return split;
	}
	
	public static String embraceEsc(String s, char c) {
		String cs = String.valueOf(c);
		return embrace(s.replace(cs, "\\" + cs), cs);
	}
	
	public static String embrace(Object o, String s) {
		return o instanceof String ? s + o.toString() + s : o.toString();
	}
	
	public static int toInt(String s) {
		return toInt(s, null);
	}
	public static int toInt(String s, Integer def) {
		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			if (def == null) throw new JapathException(e);
			else return def;
		}

	}
	
	public static class Switches {

		public static Option<String> checkSwitches(String switches, String switchPatt) {

			if (switches.trim().equals("")) return none();
			String ws = "\\s*";
			String r = ws + "(" + switchPatt + ")" + ws + "(\\," + ws + "(" + switchPatt + ")" + ")*" + ws;
			boolean b = switches.matches(r);

			return b ? none() : Option.of(Regex.getErrorString(Pattern.compile(r), switches));
		}

		public static boolean switchEnabled(String switches, String switch_) {

			return ("," + switches + ",").matches(".*\\,\\s*" + switch_ + "\\s*\\,.*");
		}

		public static boolean switchEnabled(String switches, String switch_, String switchPatt) {

			Option<String> x = checkSwitches(switches, switchPatt);
			if (x.isDefined()) throw new JapathException(x.get());
			return switchEnabled(switches, switch_);
		}
	}
	
//	public static void main(String[] args) {
//		
////		Xxx<Tuple2<String, String>> x = List.of(null);
//		
//		List x = List.of(Tuple.of("1", ""));
//		
//		System.out.println(
//		((Tuple2<String, String>) new Xxx().f(x).last())._2);
//		
//	}
	//

}
