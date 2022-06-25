package japath3.processing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import io.vavr.collection.HashMap;
import io.vavr.collection.Map;
import japath3.core.JapathException;
import japath3.schema.Schema;
import japath3.wrapper.WJsonOrg;

public class ModuleHandler {
	
	private Map<String, Module> moduleMap;
	
	private String configSchema = "_{**.::complete}.modules.every(*,  "
			+ "	and(  "
			+ "			name.type(String),  "
			+ "			opt(isSchema).type(Boolean),  "
			+ "			opt(imports).every(*,  "
			+ "				type(String))))";
	
	public ModuleHandler(File configFile) {
		init(configFile);
	}
	
	private ModuleHandler init(File configFile) {
		
		moduleMap = HashMap.empty();
		
		JSONObject joConfig;
		try {
			joConfig = new JSONObject(new JSONTokener(new FileReader(configFile)));
		} catch (JSONException | FileNotFoundException e) {
			throw new JapathException(e);
		}
		
		String mess = Schema.checkValidity(WJsonOrg.w_(joConfig), configSchema);
		if (mess != null) throw new JapathException(mess);
		
		JSONArray joModules = joConfig.getJSONArray("modules");
		for (Object o : joModules) {
			JSONObject	joModule = (JSONObject) o;
			
			String moduleName = joModule.getString("name");
			
			Path baseDir = configFile.toPath().getParent().toAbsolutePath();
			
			if (moduleMap.containsKey("name")) {
				throw new JapathException("module '" + moduleName + "' already loaded");
			}
			
			Module module;
			try {
				module = new Module(moduleName,
						new FileInputStream(baseDir.toString() + "/" + moduleName + ".ap"),
						joModule.optBoolean("isSchema"));
			} catch (Exception e) {
				throw new JapathException(e.getMessage() + " (at module '" + moduleName + "')");
			}
			
			JSONArray jaImports = joModule.optJSONArray("imports");
			
			if (jaImports != null)
				for (Object use : jaImports) {
					Module m = moduleMap.getOrElse((String) use, null);
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
