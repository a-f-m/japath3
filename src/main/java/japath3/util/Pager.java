package japath3.util;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import japath3.core.JapathException;

public class Pager<T> implements Iterable<T> {
	
	public static class AbortException extends RuntimeException {};

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
	
	public Stream<T> stream() {
		return Basics.stream(this);
	}
	
	public void punch(Function<T, T> mapper, Consumer<List<T>> puncher, int puncherSize) {
		new Buffer<T>(puncherSize, puncher).addAll(this.stream().map(mapper)).flush();
	}

	private Pager<T> setPageFunc(PageFunc<T> func) {
		this.pageFunc = func;
		currOffset = 0;
		forward(true);
		return this;
	}

	public int getPageSize() { return pageSize; }
}
