package japath3.core;

import static io.vavr.control.Option.none;

import io.vavr.control.Option;

public class JapathException extends RuntimeException {
	
	public static enum Kind {
		ConstraintViolation,
		ObjectnotFound
	}
	public Option<Kind> kind = none();

	public JapathException(Kind kind, String message) {
		super(message);
		this.kind = Option.some(kind);
	}

	public JapathException(String message) { super(message); }

	public JapathException(Throwable cause) { super(cause); }

	public JapathException() { this("irregular op  (please contact a-f-m)"); }

	public boolean equalsKind(Kind kind) { return kind.equals(this.kind.getOrNull()); }

	public JapathException setKind(Option<Kind> kind) {
		this.kind = kind;
		return this;
	}

}
