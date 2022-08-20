package japath3.processing;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TimeFuncs {

	public String germanToIsoDate(String text) {
		return LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy")).atTime(0, 0, 0).toString();
	}

	public String usToIsoDate(String text) {
		return LocalDate.parse(text.trim(), DateTimeFormatter.ofPattern("M/d/yyyy")).atTime(0, 0, 0).toString();
	}
	
	public String microsecondsToIsoDate(String text) {
		long parseLong = Long.parseLong(text.trim());
		Timestamp timestamp = new Timestamp(parseLong/1000);
		String ret = timestamp.toLocalDateTime()
				.toString();
		return ret;
	}
	
}
