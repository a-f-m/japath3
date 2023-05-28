package japath3.util;

import japath3.core.JapathException;

public class Coding {
	

	public static String UrlSpecial = " :/?#[]@!$%&'()*+,;=";
	public static String FhirSpecial = UrlSpecial + "|";
//	public static String SolrSpecialPlus = FhirSpecial + "\\+-! ():^[]\"{}~*?|&;/";

	public static String IdRegex = "[a-zA-Z0-9_]";
	
	public char escapeChar = '_';
	
	public String specialChars;
	
	// xor specialChars
	public String allowedCharsRegex;
	
	

	public Coding() {
	}

	public Coding(char c) {
		this.escapeChar = c;
	}

	public String encode(String s) {

		String enc = "";
		for (int i = 0; i < s.length(); i++) {
			Character c = s.charAt(i);
			// taken from URLEncode: we have a surrogate pair
			if (c >= 0xD800 && c <= 0xDBFF) throw new JapathException("surrogate pairs not allowed for now");
			//
			int idx;
			if (specialChars != null) {
				enc += (idx = specialChars.indexOf(c)) != -1 ? escapeChar + format(idx) + escapeChar : c;
			} else if (allowedCharsRegex != null) {
				enc += !String.valueOf(c).matches(allowedCharsRegex) || c.equals(escapeChar)
						? escapeChar + format((int) c) + escapeChar
						: c;
			} else {
				enc += c;
			}
		}
		return enc;
		
	}

	private String format(int c) { return String.format("%x", c); }

	public String decode(String s) {
		
		String dec = "";
		for (int i = 0; i < s.length(); i++) {
			Character c = s.charAt(i);
			if (c.equals(escapeChar)) {
				int end = s.indexOf(escapeChar, i + 1);
				int idx = Integer.parseInt(s.substring(i + 1, end), 16);
				dec += specialChars != null ? specialChars.charAt(idx) : (char) idx;
				i = i + end - i;
			} else {
				dec += c;
			}
		}
		return dec;
	}

	public Coding setEscapeChar(char c) {
		this.escapeChar = c;
		return this;
	}

	public Coding setSpecialChars(String s) {
		if (s.length() > 90) throw new JapathException("special chars for coding must be <= 90");
		if (allowedCharsRegex != null) throw new JapathException("specialChars XOR allowedCharsRegex");
		this.specialChars = s + escapeChar;
		return this;
	}

	public Coding setAllowedCharsRegex(String s) {
		if (specialChars != null) throw new JapathException("specialChars XOR allowedCharsRegex");
		this.allowedCharsRegex = s;
		return this;
	}
}
