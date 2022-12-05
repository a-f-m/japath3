package japath3.wrapper;

import static japath3.core.Japath.empty;
import static japath3.core.Japath.single;
import static japath3.core.Node.PrimitiveType.Any;
import static japath3.core.Node.PrimitiveType.Boolean;
import static japath3.core.Node.PrimitiveType.Number;
import static japath3.core.Node.PrimitiveType.String;

import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import japath3.core.Ctx;
import japath3.core.Japath.NodeIter;
import japath3.core.Node;
import japath3.util.JoeUtil;

public class WJsonOrg extends Node {
	
	public static boolean pretty = false;
	
	public WJsonOrg(Object wo, Object selector, Node previousNode, Ctx ctx) { super(wo, selector, previousNode, ctx); }	
	
	public static Node w_(Object x) {
		return NodeFactory.w_(x, WJsonOrg.class);
	}
	
	@Override public Node create(Object wo, Object selector, Node previousNode, Ctx ctx) {
		return new WJsonOrg(createLeafWo(wo), selector, previousNode, ctx);
	}
	
	@Override
	public Object createLeafWo(Object o) {
		return o == null ? nullWo() : o;
	}
	
	@Override public Object createWo(boolean array) { return array ? new JSONArray() : 
//		new JSONObject();
		JoeUtil.createJoe();
	}
		
	@Override
	public NodeIter get(String name) {
		
		Object o = wo instanceof JSONObject jo ? jo.opt(name) : null;
		return o == null ? empty : single(create(o, name));
	}

	@Override
	public NodeIter get(int i) {
		
		Object o = wo instanceof JSONArray ja ?  ja.opt(i) : null;
		return o == null ? empty : single(create(o, i));
	}
	
	@Override public boolean exists(Object selector) {
		return wo instanceof JSONArray ja ? //
				(selector instanceof Integer ? ja.opt((int) selector) != null : false)
				: (wo instanceof JSONObject jo ? jo.has(selector.toString()) : false);
	}
	
	@Override public Iterator<String> childrenSelectors() { return ((JSONObject) wo).keys(); }

	@Override
	public NodeIter all(Object wjo) {
		
		Node prev = this;
		
		if (wjo instanceof JSONArray ja) {
			Iterator<Object> jait = ja.iterator();
			return new NodeIter() {

				int i = 0;
				
				@Override
				public boolean hasNext() {
					return jait.hasNext();
				}

				@Override
				public Node next() {
					Node n = create(jait.next(), i, prev, ctx).setOrder(i);
					i++;
					return n;
				}
			};
		} else if (wjo instanceof JSONObject jo) {
			Iterator<String> keys = jo.keys();
			return new NodeIter() {

				int i = 0;
				
				@Override
				public boolean hasNext() {
					return keys.hasNext();
				}

				@Override
				public Node next() {
					String key = keys.next();
					return create(((JSONObject) wjo).get(key), key, prev, ctx).setOrder(i++);
				}
			};
		} else {
			return empty;
		}
	}
	
	@Override
	public Node set(String name, Object o) {
		((JSONObject) wo).put(name, o);
		return this;
	}
	@Override
	public Node set(int idx, Object o) {
		if (idx == -1) {
			((JSONArray) wo).put(o);
		} else {
			((JSONArray) wo).put(idx, o);
		}
		return this; 
	}
	
	@Override
	public void remove(Object selector) {
		if (selector instanceof String name) {
			if (wo instanceof JSONObject) set(name, null);
		} else if (selector instanceof Integer i) {
			if (wo instanceof JSONArray ja) ja.remove(i);
		}
//		if (selector.equals(s)) previousNode.set(s, null);
	}
	
	@Override public Object woCopy() {
		return wo instanceof JSONObject ? new JSONObject(wo.toString())
				: wo instanceof JSONArray ? new JSONArray(wo.toString()) : wo;
	}
	
	public static Object parse(String s) {
		return s.trim().startsWith("{") ? new JSONObject(s)
//				return s.matches("\s*\\{.+") ? new JSONObject(s)
				: new JSONArray(s);
	}
	
	@Override public Object nullWo() { return JSONObject.NULL; }
	@Override public boolean isNull() { return wo == JSONObject.NULL; }
		
	@Override
	public boolean type(PrimitiveType t) {

		switch (t) {
		case String: return wo instanceof String;
		case Number: return wo instanceof Number;
		case Boolean: return wo instanceof Boolean;
		case Any: return true;
		}
		return false;
	}
	
	@Override
	public PrimitiveType type() {

		return wo instanceof String ? String
				: wo instanceof Boolean ? Boolean //
						: wo instanceof Number ? Number : Any;
	}

	@Override public boolean isLeaf() { return !(wo instanceof JSONObject || wo instanceof JSONArray); } 
	@Override public boolean isArray() { return wo instanceof JSONArray; } 
	
	@Override public String woString(int indent) { 
		return (wo instanceof JSONObject jo ? jo.toString(indent)
				: wo instanceof JSONArray ja ? ja.toString(indent) : wo.toString());
	}
	
	@Override
	public String toString() {
		
		return "`" + selector + "`->" + woString(pretty ? 3 : 0);
	}
	
	
}