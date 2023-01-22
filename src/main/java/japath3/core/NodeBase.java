package japath3.core;

import japath3.core.Japath.NodeIter;

public abstract class NodeBase {

	public abstract NodeIter get(String name);
	public abstract NodeIter get(int i);
	
	public abstract Node set(String name, Object o);
	public abstract Node set(int idx, Object o);
	
	public abstract void remove(Object selector);
	
	public abstract NodeIter all(Object o);
	
	public abstract boolean isLeaf();
	public abstract boolean isArray();
	public abstract Object woCopy();


}
