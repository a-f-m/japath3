package japath3.util;

import java.io.IOException;
import java.io.Reader;
import java.util.function.Consumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import japath3.core.JapathException;


public class Csv {

	public static int visit(Reader reader, char delimiter, Consumer<CSVRecord> consumer) {
		
		try (CSVParser parser = new CSVParser(reader, CSVFormat.EXCEL.withDelimiter(delimiter).withHeader())) {

			int lineNo = 0;
			for (CSVRecord record : parser) {
				lineNo++;
				consumer.accept(record);
			}
			return lineNo;
		} catch (IOException e) {
			throw new JapathException(e);
		}
	}
	

}
