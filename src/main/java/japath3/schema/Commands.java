package japath3.schema;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.io.IOUtils;

import japath3.cli.CliBase;
import japath3.wrapper.NodeFactory;
import japath3.wrapper.WGson;

public class Commands {

	public static void main(String[] args) {

		NodeFactory.setDefaultWrapperClass(WGson.class);

		CliBase cli = new CliBase("java -Dfile.encoding=UTF-8 -cp target/japath-cli.jar  japath3.schema.Commands",
				"""
				generates json schema and examples from prototype file
				options:		
				"""
				);
		cli.optHint = "";
		
//		cli.exaText = exa;

		cli.options
				.addOption(Option.builder("p")
						.longOpt("prototypes")
						.desc("file containing the prototypes")
						.hasArg()
						.argName("json-prototype-file")
						.build())
				.addOption(Option.builder("t")
						.longOpt("targetDir")
						.desc("target directory where the generated files are written to")
						.hasArg()
						.argName("target-dir")
						.build())
				.addOption(Option.builder("e")
						.longOpt("example")
						.desc("example call and prototypes")
						.build())
				;

		try {

			CommandLine cmd = cli.parse(args);
			if (cmd != null) {
				
				if (cmd.hasOption("e")) {
					System.out.println(exa);
					return;
				}
				
				String prototypeFile = cmd.getOptionValue("p", null);
				if (prototypeFile == null) throw new Exception("prototype file missing");
				System.out.println("prototypes: " + prototypeFile);
				String targetDir = cmd.getOptionValue("t", ".");
				System.out.println("target directory: " + targetDir);

				var jsp = new JsonSchemaProcessing().setComplete(false)
						.usePrototypeBundle(NodeFactory.w_(IOUtils.toString(new FileInputStream(prototypeFile), "utf-8")));
				String f = targetDir + "/schema.json";
				var pw = new PrintWriter(new FileOutputStream(f));
				pw.write(jsp.getSchemaBundle().woString(3));
				pw.close();
				System.out.println("json schema file '" + f + "' generated");
				f = targetDir + "/examples.json";
				pw = new PrintWriter(new FileOutputStream(f));
				pw.write(jsp.getResolvedPrototypeBundle().woString(3));
				pw.close();
				System.out.println("examples file '" + f + "' generated");

			}

		} catch (Exception e) {
			System.err.println("---");
			System.err.println(e.getMessage().replace("\r", " "));
			System.exit(1);
		}

	}
	
	private static String exa  = """
			
*** example ***

example call: java -Dfile.encoding=UTF-8 -cp target/japath-cli.jar  japath3.schema.Commands -p "./prototypes.jsonc" -t "./generated"

{
	// optional json schema injection. properties not prefixed with $proto
	// are directly injected (e.g. 'additionalProperties') 
    "$proto:injections:json-schema": [
        {
			// the target path to $defs below for injections
			// if you want to inject deep, use "Person.**"
            "$proto:targets": "Person",
            // classical json schema keyword injection
            "additionalProperties": false,
            // properties matching regex are optional
            "$proto:optional": "personal|fav.*" 
        }
    ],
    // within $defs are the types (e.g. Person) and the corresponding prototype (~ json example)
    "$defs": {
        "Person": {
            "personal": {
                "name": "Miller",
                "age": 17
            },
            "favorites": [ "coen-brothers", "dylan" ]
        },
        "Project": {
            "name": "proj1",
            "lead": {
				// reference to other types allows for modularization
                "$ref": "#/$defs/Person"
            }
        }
    }
}			
			""";
}
