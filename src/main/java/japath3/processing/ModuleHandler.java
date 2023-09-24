package japath3.processing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

import org.apache.commons.io.IOUtils;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import japath3.core.Ctx;
import japath3.core.JapathException;
import japath3.core.Node;
import japath3.schema.Schema;
import japath3.wrapper.NodeFactory;

public class ModuleHandler {
	
	public static record NsFuncs(String ns, Object funcsObj) {};
	
	private Map<String, Module> moduleMap;
	
	private String configSchema = "_{**.::complete}.modules.every(*,  "
			+ "	and(  "
			+ "			name.type(String),  "
			+ "			opt(isSchema).type(Boolean),  "
			+ "			opt(imports).every(*,  "
			+ "				type(String))))";
	
	public ModuleHandler(File configFile, NsFuncs... nsFuncs) {
		init(configFile, nsFuncs);
	}
	
	private ModuleHandler init(File configFile, NsFuncs... nsFuncs) {
		
		for (NsFuncs nsf : nsFuncs) Ctx.loadJInst(nsf.ns, nsf.funcsObj);
		
		moduleMap = HashMap.empty();
		
		Node joConfig;
		try {
//			joConfig = new JSONObject(new JSONTokener(new FileReader(configFile)));
			joConfig = NodeFactory.w_(IOUtils.toString(new FileReader(configFile)));
//		} catch (JSONException | FileNotFoundException e) {
		} catch (IOException e) {
			throw new JapathException(e);
		}
		
//		String mess = Schema.checkValidity(NodeFactory.w_(joConfig, WJsonOrg.class) , configSchema);
		String mess = Schema.checkValidity(joConfig , configSchema);
		if (mess != null) throw new JapathException(mess);
		
		Node joModules = joConfig.node("modules");
		for (Node o : joModules.all()) {
//			JSONObject	joModule = (JSONObject) o;
			
//			String moduleName = joModule.getString("name");
			String moduleName = o.val("name");
			
			Path baseDir = configFile.toPath().getParent().toAbsolutePath();
			
			if (moduleMap.containsKey("name")) {
				throw new JapathException("module '" + moduleName + "' already loaded");
			}
			
			Module module;
			try {
				module = new Module(moduleName,
						new FileInputStream(baseDir.toString() + "/" + moduleName + ".ap"),
						o.val("isSchema", false));
			} catch (Exception e) {
				throw new JapathException(e.getMessage() + " (at module '" + moduleName + "')");
			}
			
			Node jaImports = o.node("imports");
			
			if (jaImports != Node.nil)
				for (Node use : jaImports.all()) {
					Module m = moduleMap.getOrElse(use.val(), null);
					if (m == null) throw new JapathException("module '" + use + "' not known");
					module.importModule(m);				
				}
			
			moduleMap = moduleMap.put(moduleName, module);
		}
		
		return this;
		
	}
	
	public Module getModule(String name) {
		
		if (!moduleMap.containsKey(name)) throw new JapathException("module '" + name + "' not known");
		return moduleMap.get(name).get();
		
	}

}
