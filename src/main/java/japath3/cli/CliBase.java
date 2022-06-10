package japath3.cli;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.florianingerl.util.regex.Matcher;

import japath3.core.JapathException;
import japath3.util.Regex;

public class CliBase {
	
	public Options options;
	String command;
	String htext;
	
	public CliBase(String command, String htext) { 
		this.command = command; 
		this.htext = htext;
		options = defaultOptions();
	}

	public static String getCommand(String[] args, String regex) {
		
		if (args.length == 0) throw new JapathException("empty args");
		Matcher m = Regex.match(regex, args[0]);
		if (m == null) throw new JapathException("command must conform to " + regex);
		String c = m.group(1);
		return c;
	}

	public static Options defaultOptions() {
		return new Options().addOption(Option.builder().longOpt("help").desc("this help").build());
	}

	public CommandLine parse(String[] args) {

//		if (args.length == 0) {
//			System.err.println("no args given");
//			help(options, command);
//			return null;
//		} else 
		{
			try {
				CommandLine cmd = new DefaultParser().parse(options, args, false);
				if (cmd.hasOption("help")) {
					help();
					return null;
				} else {
					return cmd;
				}
			} catch (ParseException e) {
				System.err.println("error: " + e.getLocalizedMessage());
				help();
				return null;
			}
		}
	}

	public void help() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(120,
				command,
				"\n"
				+ htext
				+ "\n\n!!! An option-argument value below prefixed with 'd:' is the default value\n",
				options,
				"",
				true);
	}
	
	public String optVal(CommandLine cmd, String opt, String regex, String def) {

		String v = cmd.getOptionValue(opt, def);
		if (v != null && !v.matches(regex)) {
			System.err.println("'" + opt + "'-argument '" + v + "' does not match (" + regex + ")");
			help();
			System.exit(1);
		}
		return v;
	}

}
