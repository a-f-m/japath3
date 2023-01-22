package japath3.wrapper;

import static japath3.core.Japath.empty;
import static japath3.core.Japath.nodeIter;
import static japath3.core.Japath.single;
import static japath3.core.Japath.singleObject;

import java.util.ArrayList;
import java.util.Iterator;

import org.jsoup.nodes.Element;

import japath3.core.Ctx;
import japath3.core.Japath.NodeIter;
import japath3.core.Node;

public class WJsoup extends Node {

	public WJsoup(Object wo, Object selector, Node previousNode, Ctx ctx) { super(wo, selector, previousNode, ctx); }
	
	public static Node w_(Object x) { return w_(x, "", null, new Ctx()); }
	public static Node w_(Object x, Object selector, Node previousNode, Ctx ctx) {
		return new WJsoup(x, selector, previousNode, ctx);
	}

	@Override public NodeIter text() {

		return single(w_(wo instanceof Element ? ((Element) wo).ownText() : wo.toString(), "text", this, ctx));
	}

	@Override public NodeIter get(String name) {
		
		if (!(wo instanceof Element)) return empty;
		Element elm = (Element) wo;
		
		String attr = elm.attr(name);
		if (!attr.equals("")) {
			return singleObject(attr, this);
		}
		
		Iterator<Element> iterator = elm.children().stream().filter(x -> {
			return x.tagName().equals(name);
		}).iterator();

		Node prev = this;

		return new NodeIter() {

			int i = 0;

			@Override public boolean hasNext() { return iterator.hasNext(); }

			@Override public Node next() { return w_(iterator.next(), name, prev, ctx).setOrder(i++); }
		};

	}
	
	@Override public boolean isAttribute(String name) {
		
		if (wo instanceof Element) {
			
			Element elm = (Element) wo;
			return elm.hasAttr(name);
		}
		return false;
	}
	
	@Override public NodeIter get(int i) {

		if (!(wo instanceof Element)) return empty;
		Element elm = (Element) wo;

		Element ret = null;
		try {
			ret = elm.child(i);
		} catch (IndexOutOfBoundsException e) {
//			throw new JapathException("subscript out of range at '" + this + "'");
		}
		return ret == null ? empty : single(w_(ret, i, this, ctx));
	}

	@Override public NodeIter all(Object wjo) {

		if (!(wjo instanceof Element)) return empty;
		Iterator<Element> iterator = ((Element) wjo).children().iterator();

		Node prev = this;

		return new NodeIter() {

			int i = 0;

			@Override public boolean hasNext() { return iterator.hasNext(); }

			@Override public Node next() {
				Element elm = iterator.next();
				Node ret = w_(elm, elm.tagName(), prev, ctx).setOrder(i);
				i++;
				return ret;
			}
		};
	}

	@Override public NodeIter desc() {

		ArrayList<Node> descs = new ArrayList<Node>();
		gatherDesc(descs, w_(wo, selector, this.previousNode, ctx));
		return nodeIter(descs.iterator());
	}
	
	@Override public Object woCopy() { return wo; }

//	@Override public boolean isLeaf() { return !(wo instanceof Element); }
	@Override public boolean isLeaf() { return !(wo instanceof Element) || ((Element) wo).children().isEmpty(); }

	@Override public boolean isArray() { return false; }
	
	@Override public PrimitiveType type() { return PrimitiveType.String; }
	
	@Override public Node set(String name, Object o) { throw new UnsupportedOperationException(); }
	@Override public Node set(int idx, Object o) { throw new UnsupportedOperationException(); }
	@Override public void remove(Object selector) { throw new UnsupportedOperationException(); }

}
