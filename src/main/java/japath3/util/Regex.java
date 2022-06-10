package japath3.util;

import java.util.List;

import com.florianingerl.util.regex.CaptureTreeNode;
import com.florianingerl.util.regex.Matcher;
import com.florianingerl.util.regex.Pattern;
import com.florianingerl.util.regex.PatternSyntaxException;

public class Regex {

	public static String group(Matcher m, int group) {

		try {
			return m == null ? null : m.group(group);
		} catch (IndexOutOfBoundsException | IllegalStateException e) {
			return null;
		}
	}

	public static Matcher match(String regex, String s) {
		return match(Pattern.compile(regex), s);
	}

	public static Matcher match(Pattern p, String s) {
		return match(p, s, false);
	}

	public static Matcher match(Pattern p, String s, boolean captureTree) {
		Matcher m = p.matcher(s);
		if (captureTree) m.setMode(Matcher.CAPTURE_TREE);
		return m.matches() ? m : null;
	}

	// used with Matcher.find()
	public static String nextMatch(Matcher m, String s) {
		return s.substring(m.start(), m.end());
	}

	public static List<CaptureTreeNode> getCaptureTreeNodes(Matcher m) {
		return m.captureTree().getRoot().getChildren();
	}

	// from 'https://stackoverflow.com/users/1369991/andreas-mayer'
	public static int indexOfLastMatch(Pattern pattern, String input) {
		Matcher matcher = pattern.matcher(input);
		for (int i = input.length(); i > 0; --i) {
			Matcher region = matcher.region(0, i);
			if (region.matches() || region.hitEnd()) {
				return i;
			}
		}
		return 0;
	}

	public static String getErrorString(Pattern p, String input) {
		
		int idx = Regex.indexOfLastMatch(p, input);
		return input.substring(0, idx) + " >>> unexpected input >>> " + input.substring(idx);
	}
	
	public static String check(String r) {
		
		try {
         Pattern.compile(r);
         return null;
     } catch (PatternSyntaxException exception) {
         return exception.getDescription();
     }
	}
	
	public static boolean isTrueRegex(String r) {
		
		// $ - \ are not neccessary
		String specialChar = "<([{^=!|]})?*+.>";
		for (int i = 0; i < r.length(); i++) {
			if (specialChar.indexOf(r.charAt(i)) != -1) return true;
		}
		return false;
	}

	public static String extract(String input, String regex, String def) {

		Matcher matcher = Pattern.compile(regex).matcher(input);
		return matcher.find() ?  matcher.group(1).trim() : def;
	}
	
	public static String[] multiExtract(String input, String regex, String... defaults) {
		
		Matcher matcher = Pattern.compile(regex).matcher(input);
		if (!matcher.matches()) return null;
		int cnt = matcher.groupCount() + 1;
		String[] ret =  new String[cnt];
		
		if (defaults == null || defaults.length == 0) defaults = new String[cnt];
//		if (cnt != defaults.length) throw new JapathException("groups != defaults or no defaults");
		

		for (int i = 0; i < ret.length; i++) {
			String g = matcher.group(i);
			ret[i] = g == null ? null : g.trim();
			if (ret[i] == null) ret[i] = defaults[i];
		}
		
		return ret;
	}
}
