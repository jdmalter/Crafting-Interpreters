package lox;

@SuppressWarnings("serial")
public class RuntimeError extends RuntimeException {

	private final Token token;

	public RuntimeError(Token token, String message) {
		super(message);
		this.token = token;
	}

	public Token token() {
		return token;
	}

}
