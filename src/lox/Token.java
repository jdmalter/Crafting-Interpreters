package lox;

public class Token {

	/** How the current token should be interpreted. */
	private final TokenType type;
	/** The smallest sequence that still represents something. */
	private final String lexeme;
	/** The value of the token. */
	private final Object literal;
	/** What line contains the current token. */
	private final int line;

	/**
	 * 
	 * @param type
	 *            How the current token should be interpreted.
	 * @param lexeme
	 *            The smallest sequence that still represents something.
	 * @param literal
	 *            The value of the token.
	 * @param line
	 *            What line contains the current token.
	 */
	public Token(TokenType type, String lexeme, Object literal, int line) {
		this.type = type;
		this.lexeme = lexeme;
		this.literal = literal;
		this.line = line;
	}

	/**
	 * @return How the current token should be interpreted.
	 */
	public TokenType type() {
		return type;
	}

	/**
	 * @return The smallest sequence that still represents something.
	 */
	public String lexeme() {
		return lexeme;
	}

	/**
	 * @return The value of the token.
	 */
	public Object literal() {
		return literal;
	}

	/**
	 * @return What line contains the current token.
	 */
	public int line() {
		return line;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "{:type " + type + " :lexeme " + lexeme + " :literal " + literal + " :line " + line + "}";
	}

}
