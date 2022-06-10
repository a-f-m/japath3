package japath3.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

public class Buffer<T> implements Iterable<T> {

	private List<T> buffer;
	private int buffSize;
	private int cnt;
	private int total;

	private Consumer<List<T>> initialPuncher = x -> {
		throw new UnsupportedOperationException("puncher undefined");
	};

	public Buffer(int buffSize) { this.buffSize = buffSize; buffer = new ArrayList<>(buffSize); }

	public Buffer(int buffSize, Consumer<List<T>> puncher) { this(buffSize); this.initialPuncher = puncher; }

	@Override
	public Iterator<T> iterator() { return buffer.iterator(); }

	public void add(T... xs) { for (T x : xs) add(x); }

	public Buffer<T> add(T x) { return add(x, initialPuncher); }

	public Buffer<T> add(T x, Consumer<List<T>> puncher) {

		cnt++;
		total++;
		buffer.add(x);
		flush(puncher, false);
		return this;
	}

	public void flush() { flush(initialPuncher); }

	public void flush(Consumer<List<T>> puncher) { flush(puncher, true); }

	private void flush(Consumer<List<T>> puncher, boolean force) {

		if (force || cnt == buffSize) {
			puncher.accept(buffer);
			cnt = 0;
			buffer.clear();
		}
	}

	public int getTotal() { return total; }

}
