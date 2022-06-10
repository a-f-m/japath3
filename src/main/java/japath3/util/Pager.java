package japath3.util;

import java.util.Iterator;

import japath3.core.JapathException;

public class Pager<T> implements Iterable<T> {

	@FunctionalInterface
	public static interface PageFunc<T> {

		public Iterator<T> apply(int offset, int limit);
	}

	private int pageSize;
	private int currOffset;
	private Iterator<T> currIter;

	PageFunc<T> pageFunc;

	public Pager(int pageSize) { this.pageSize = pageSize; }

	public Pager(int pageSize, PageFunc<T> func) {
		this(pageSize);
		setPageFunc(func); 
	}

	private boolean forward(boolean ini) {

		if (!ini) currOffset += pageSize;
		try {
			currIter = pageFunc.apply(currOffset, pageSize);
			return currIter.hasNext();
		} catch (Exception e) {
			throw new JapathException(e);
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {

			@Override
			public boolean hasNext() {

				boolean hasNext = currIter.hasNext();
				if (!hasNext) hasNext = forward(false);
				return hasNext;
			}

			@Override
			public T next() { return currIter.next(); }

		};
	}

	private Pager<T> setPageFunc(PageFunc<T> func) {
		this.pageFunc = func;
		currOffset = 0;
		forward(true);
		return this;
	}
}
