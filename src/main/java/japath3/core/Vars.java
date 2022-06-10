package japath3.core;


import org.json.JSONArray;
import org.json.JSONObject;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.LinkedHashMap;
import io.vavr.collection.Map;
import japath3.core.Japath.Bind;

public class Vars {

	private Map<String, Var> varMap = LinkedHashMap.empty();
	public Map<String, Bind> firstOccs = HashMap.empty();

	public Var getVar(String name) { return varMap.getOrElse(name, null); }
	
	public Map<String, Var> getVarMap() {
		return varMap.filter(x -> {
			return !x._1.equals("$");
		});
	}

	public Tuple2<Var, Boolean> register(String name, Bind bind) {
		
		Var v = getVar(name);
		if (v == null) {
			add(v = new Var(), name);
			firstOccs = firstOccs.put(name, bind);
			return Tuple.of(v, true);
		} else {
			return Tuple.of(v, false);
		}
	}
	
	public Vars add(Var var, String name) {
		varMap = varMap.put(name, var);
		return this;
		
	}
	
	public <T> Var<T> v(String name) {
		Var v = getVar(name);
		return v == null ? Var.of() : v;
	}
	
	public <T> T val(String name) {
		
		return (T) v(name).val();
		
	}

	public boolean firstOcc(String name, Bind bind) { 
		boolean contains = firstOccs.contains(Tuple.of(name, bind));
		return contains; 
	}
	
	public void clearVars() {
		varMap = LinkedHashMap.empty();
		firstOccs = HashMap.empty();
	}
	
	public JSONObject toJson() {
		
		JSONArray vars = new JSONArray();
		for (Tuple2<String, Var> v : getVarMap()) {
			vars.put(new JSONObject().put(v._1, v._2.val()));
		}
		return new JSONObject().put("vars", vars);
		
	}

	
	@Override public String toString() {
		return getVarMap().mkString(",");
	}
}
