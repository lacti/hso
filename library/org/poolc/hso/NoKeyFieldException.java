package org.poolc.hso;

public class NoKeyFieldException extends RuntimeException {

	private static final long serialVersionUID = -4536023481387644588L;

	public NoKeyFieldException() {
		super("Object doesn't contain key field");
	}

	public NoKeyFieldException(String message, Throwable cause) {
		super(message, cause);
	}

	public NoKeyFieldException(String message) {
		super(message);
	}

	public NoKeyFieldException(Throwable cause) {
		super(cause);
	}
}
