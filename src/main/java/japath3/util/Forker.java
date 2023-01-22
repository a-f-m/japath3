package japath3.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class Forker<Result>  {

	@FunctionalInterface
	public static interface Fork<Input, Result> {
		public Iterator<Result> eval(Forker f, Input input);
	}
	
	public List<Forker.Fork> forks;

	public Forker(List<Forker.Fork> forks) { this.forks = forks; }
	public Iterator<Result> eval() { return eval(forks, null); }
	private Iterator<Result> eval(List<Forker.Fork> forks, Result input) {
		if (forks == null || forks.size() == 0) {
			return single(input);
		} else {
			Iterator<Result> itY = forks.get(0).eval(this, input);
			return new Iterator() {
				Iterator<Result> itZ = Collections.emptyIterator();
				@Override public boolean hasNext() {

					loop: while (true) {

						if (!itZ.hasNext()) {
							if (itY.hasNext()) {
								List<Forker.Fork> tail = forks.size() == 1 ? null : forks.subList(1, forks.size());
								Result next = itY.next();
								itZ = eval(tail, next);
								if (itZ.hasNext()) {
									return true;
								} else {
									continue loop;
								}
							} else {
								return false;
							}
						} else {
							return true;
						}
					}
				}
				@Override public Result next() { return itZ.next(); }
			};
		}
	}

	@Override public String toString() { return "forks" + List.of(forks); }

	public static <Result> Iterator<Result> single(Result input) {
		return new Iterator() {

			boolean consumed;

			@Override public boolean hasNext() { return !consumed; }
			@Override public Result next() {
				consumed = true;
				return input;
			}
		};
	}

}