package japath3.cli;

import static japath3.core.Japath.walki;
import static japath3.processing.Language.e_;
import static japath3.schema.Schema.MessageHandling.None;
import static japath3.schema.Schema.MessageHandling.Only;
import static japath3.schema.Schema.MessageHandling.Prefer;
import static japath3.util.Basics.stream;
import static japath3.util.Basics.Switches.checkSwitches;
import static japath3.util.Basics.Switches.switchEnabled;
import static japath3.util.JoeUtil.createJoe;
import static java.util.stream.Collectors.joining;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import io.vavr.Tuple2;
import japath3.core.JapathException;
import japath3.core.JapathException.Kind;
import japath3.core.Node;
import japath3.core.Var;
import japath3.core.Vars;
import japath3.schema.Schema;
import japath3.schema.Schema.Mode;
import japath3.service.HttpService;
import japath3.util.JoeUtil;
import japath3.wrapper.NodeFactory;
import japath3.wrapper.WJsonOrg;
import japath3.wrapper.WJsoup;

//String c = CliBase.getCommand(args, "(select|schema)");
//
//String[] params = Arrays.copyOfRange(args, 1, args.length);

public class Commands {

	private static Schema reqSchema;

	static {
		reqSchema = new Schema();
		reqSchema.genSelectorRestriction(true).setSchema(Commands.class.getResourceAsStream("reqSchema.ap"));
	}

	public static void main(String[] args) {

		// System.out.println(Arrays.asList(args));
		// System.out.println(List.of(args).mkString(" "));

		CliBase cli = new CliBase("java -Dfile.encoding=UTF-8 -jar japath-cli.jar",
				"General apath operation that consumes input (via stdin or <text>: Json, Xml/Html), "
						+ "ckecks it against constraints/schema <constraints-expr>, evaluates the expression <apath-expr>, and "
						+ "(optionally) get the value of variable <name>. Let R be the result then\n"
						+ "- R will be outputted (op=select)\n"
						+ "- a schema of R will be generated (op=schemaGen)\n"
						+ "- a select expression of R will be generated (op=selectGen)");

		Option o = Option.builder()
				.longOpt("var")
				.desc("optional variable name. if <name> is missing all variables are retrieved")
				.hasArg()
				.optionalArg(true)
				.argName("name")
				.build();
		cli.options
				.addOption(Option.builder()
						.longOpt("op")
						.desc("command (see above)")
						.hasArg()
						.argName("d:select|schemaGen|selectGen")
						.build())
				.addOption(Option.builder()
						.longOpt("type")
						.desc("source language type")
						.hasArg()
						.argName("d:json|xml")
						.build())
				.addOption(Option.builder().longOpt("stdin").desc("input (json, ...) read from stdin").build())
				.addOption(Option.builder().longOpt("salient").desc("salient mode: checking selector usage").build())
				.addOption(Option.builder().longOpt("text").desc("input if --stdin not set").hasArg().argName("text").build())
				.addOption(Option.builder()
						.longOpt("apathExpr")
						.desc("apath expression. if missing the input itself will be processed")
						.hasArg()
						.argName("apath-expr")
						// .required()
						.build())
				.addOption(
						Option.builder().longOpt("constraints").desc("optional apath constraints/schema").hasArg().argName("constraints-expr").build())
				.addOption(Option.builder().longOpt("pretty").desc("pretty output").build())
				.addOption(Option.builder().longOpt("asArray").desc("output as an json array ([...])").build())
				.addOption(o);
		Option optVar = Option.builder()
				.longOpt("schemaGenSwitches")
				.desc("optional comma-separated switches (w/o whitespace) for schema generation (default: all off). "
						+ "'opt': all selectors (properties) are optional"
//						+ "'selectorRestriction': only selectors of input allowed"
						)
				.hasArg()
				.argName("s1,...")
//				.numberOfArgs(2)
				.build();
		cli.options.addOption(optVar);
		cli.options
		.addOption(Option.builder()
				.longOpt("service")
				.desc("starts the http service (default port 8081); !!! all other options are not considered")
				.hasArg()
				.optionalArg(true)
				.argName("port")
				.build());
		try {

			CommandLine cmd = cli.parse(args);
			if (cmd != null) {

				boolean asService = cmd.hasOption("service");
				if (asService) {
					String port = cmd.getOptionValue("service");
					HttpService.main(new String[] { port == null ? "8081" : port  });
				}
				
				String op = cli.optVal(cmd, "op", "select|schemaGen|selectGen", "select");
				String type = cli.optVal(cmd, "type", "json|xml", "json");

				// System.out.println(op);
				String text = "";
				if (cmd.hasOption("stdin")) {
					BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
					String line;
					while ((line = br.readLine()) != null) {
						text += line + "\n";
					}
					br.close();
				} else {
					text = cmd.getOptionValue("text");
					if (text == null) {
						System.err.println("no input text given");
						cli.help();
						System.exit(1);
					}
				}
				
				JSONObject request = createJoe()
						.put("_op", op)
						.put("type", type)
						.put("_body", text)
						.put("apathExpr", cmd.getOptionValue("apathExpr", null))
						.put("var",
								!cmd.hasOption("var") ? null : cmd.getOptionValue("var") == null ? "" : cmd.getOptionValue("var"))
						.put("constraints", cmd.getOptionValue("constraints"))
						.put("pretty", cmd.hasOption("pretty"))
						.put("asArray", cmd.hasOption("asArray"))
						.put("schemaGenSwitches", cmd.getOptionValue("schemaGenSwitches", null))
						.put("salient", cmd.hasOption("salient"))
						;

				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
				String output = exec(request);
				bw.write(output);
				bw.close();

			}
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			// CliBase.help(options, command);
			System.exit(1);
		}
	}

	public static String optPretty(boolean pretty, Tuple2<Object, Boolean> result) {
		return result._2 ? JoeUtil.prettyString(result._1, pretty ? 3 : 0) : result._1.toString();
		// return result._2 && result._1 instanceof JSONObject ? ((JSONObject)
		// result._1).toString(pretty ? 3 : 0)
		// : result._1.toString();
	}

	public static String exec(JSONObject request) {

		Node nReq = NodeFactory.w_(request, WJsonOrg.class);
		// System.out.println(request.toString(3));
		// System.out.println(schema.getSchemaExpr());
		// System.out.println(new Schema().buildConstraintText(nReq));

		if (!reqSchema.checkValidity(nReq)) {
			throw new JapathException(reqSchema.annotatedViolations(nReq));
			// return Tuple.of(reqSchema.annotatedViolations(nReq), false);
		}

		String oOp = request.optString("_op", "select");
		String oType = request.optString("type", "json");
		String t = "json|xml";
		if (!oType.matches(t)) {
			throw new JapathException("--type: one of (" + t + ") ");
		}
		String oVar = request.optString("var", null);
		String oSchema = request.optString("constraints", null);
		int ind = request.optBoolean("pretty") ? 3 : 0;

		Object body = request.get("_body");
		Node n = buildNode(body, oType);
		n.ctx.setSalient(request.optBoolean("salient"));
		// Implicitly throws exception at viol
		checkSchema(oSchema, oType, n, request.optBoolean("genMessages"), request.optBoolean("genMessagesOnly"));

		JSONArray results = new JSONArray();
		// for (Node node : nodes) {
		Iterable<Node> walki = null;
		try {
			walki = walki(n, e_(request.optString("apathExpr", "?(true)")));
		} catch (JapathException e) {
			throw new JapathException("--apathExpr: " + e.getMessage());
		}
		for (Node node : walki) {

			Vars vars = n.ctx.getVars();
			if ("".equals(oVar)) {
				// results.put(vars.toString());
				results.put(vars.toJson());
				continue;
			}
			Var v = vars.v(oVar);
			// if (oVar != null && !v.bound()) throw new JapathException("var '" +
			// oVar + "' not bound");

			switch (oOp) {
			case "select":
				results.put(oVar != null ? v.valClone() : node.val());
				break;
			case "schemaGen":
			case "selectGen":
				String optString = request.optString("schemaGenSwitches", null);
				String switchPatt = "opt|selectorRestriction";
				if (optString != null) {
					io.vavr.control.Option<String> c = checkSwitches(optString, switchPatt);
					if (c.isDefined()) throw new JapathException("--schemaGenSwitches: " + c.get());
				}

				results.put(new Schema().genOpt(switchEnabled(optString, "opt"))
						.genSelectorRestriction(switchEnabled(optString, "selectorRestriction"))
						.setMode(oOp.equals("schemaGen") ? Mode.SchemaMode : Mode.SelectMode)
						.buildConstraintText(oVar != null ? v.node() : node));
				break;
			default:
				throw new JapathException("command '" + oOp + "' not implemented");
			}
		}
		return !request.optBoolean("asArray") ? //
				stream(results).map(x -> {
					return JoeUtil.prettyString(x, ind);
				}).collect(joining("\n")) //
				//
				: JoeUtil.prettyString(results, ind);

	}

	public static Node buildNode(Object body, String oType) {
		
		Node n = null;
		if (body instanceof String s && s.charAt(0) == '-') {
			try {
				body =  IOUtils.toString(new FileReader(s.substring(1))).replace("/", "\\/")  ;
			} catch (IOException e) {
				throw new JapathException(e);
			}
//			body = IOUtils.toString(new FileReader(body.substring(1);
		}

		if (oType.equals("json")) {
			n = NodeFactory.w_(body instanceof JSONObject ? body : createJoe(body.toString()), WJsonOrg.class);
		} else { // xml
			Document doc = Jsoup.parse(body.toString(), "", Parser.xmlParser());
			if (doc.children().isEmpty()) throw new JapathException("no tags given");
			n = WJsoup.w_(doc);
		}
		return n;
	}

	private static void checkSchema(String oSchema, String oType, Node n, boolean genMessages, boolean genMessagesOnly) {
		if (oSchema != null) {
			if (oType.equals("xml")) throw new JapathException("no schema for 'xml' allowed");
			String viol = null;
			try {
				viol = new Schema().setSchema(oSchema)
						.genMessages(genMessages ? ( genMessagesOnly ? Only : Prefer) : None)
						.getValidityViolations(n)
						.getOrNull();
			} catch (JapathException e) {
				throw new JapathException("--constraints/schema: " + e.getMessage());
			}
//			if (viol != null) throw new JapathException(Kind.ConstraintViolation, "Constraints violated. Possible corrections ('!!!'-annotations) below\n" + viol);
			if (viol != null) throw new JapathException(Kind.ConstraintViolation, "// Constraints violated. Possible corrections ('\u2190 ...') below\n" + viol);
		}
	}
}
