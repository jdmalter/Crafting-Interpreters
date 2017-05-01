package lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static lox.TokenType.*;

public class Scanner {

	/** The map of the keyword's lexeme to the keyword's token type. */
	private static final Map<String, TokenType> keywords = new HashMap<String, TokenType>();

	/**
	 * Add some keywords to
	 */
	static {
		keywords.put("and", AND);
		keywords.put("class", CLASS);
		keywords.put("else", ELSE);
		keywords.put("false", FALSE);
		keywords.put("for", FOR);
		keywords.put("fun", FUN);
		keywords.put("if", IF);
		keywords.put("nil", NIL);
		keywords.put("or", OR);
		keywords.put("print", PRINT);
		keywords.put("return", RETURN);
		keywords.put("super", SUPER);
		keywords.put("this", THIS);
		keywords.put("true", TRUE);
		keywords.put("var", VAR);
		keywords.put("while", WHILE);
	}

	/** Some source code. */
	private final String source;
	/** The ordered list of tokens generated from some source code. */
	private final List<Token> tokens = new ArrayList<Token>();
	/** The offset of the first character in the current lexeme. */
	private int start = 0;
	/** The offset of the character under consideration. */
	private int current = 0;
	/** The current line count. */
	private int line = 1;

	/**
	 * 
	 * @param source
	 *            Some source code.
	 */
	public Scanner(String source) {
		this.source = source;
	}

	/**
	 * 
	 * @return The ordered list of tokens generated from some source code.
	 */
	public List<Token> scanTokens() {
		while (!isAtEnd()) {
			start = current;
			scanToken();
		}

		tokens.add(new Token(EOF, "", null, line));
		return new ArrayList<>(tokens);
	}

	private void scanToken() {
		char c = advance();
		switch (c) {

		// Single-character tokens.
		case '(':
			addToken(LEFT_PAREN);
			break;
		case ')':
			addToken(RIGHT_PAREN);
			break;
		case '{':
			addToken(LEFT_BRACE);
			break;
		case '}':
			addToken(RIGHT_BRACE);
			break;
		case ',':
			addToken(COMMA);
			break;
		case '.':
			addToken(DOT);
			break;
		case '-':
			addToken(MINUS);
			break;
		case '+':
			addToken(PLUS);
			break;
		case ';':
			addToken(SEMICOLON);
			break;
		case '*':
			addToken(STAR);
			break;
		case '?':
			addToken(QUESTION);
			break;
		case ':':
			addToken(COLON);
			break;

		// One or two character tokens.
		case '!':
			addToken(match('=') ? BANG_EQUAL : BANG);
			break;
		case '=':
			addToken(match('=') ? EQUAL_EQUAL : EQUAL);
			break;
		case '<':
			addToken(match('=') ? LESS_EQUAL : LESS);
			break;
		case '>':
			addToken(match('=') ? GREATER_EQUAL : GREATER);
			break;

		// operator and comments being with a slash
		case '/':
			if (match('/')) {
				// A comment goes until the end of the line.
				while (peek() != '\n' && !isAtEnd()) {
					// Consume newline
					advance();
				}
			} else if (match('*')) {
				while (peek() != '*' && peekNext() != '/' && !isAtEnd()) {
					if (peek() == '\n') {
						line++;
					}
					advance();
				}

				// Unterminated block comment
				if (isAtEnd()) {
					Lox.error(line, "Unterminated block comment.");
				}

				// Closing */
				advance();

				if (isAtEnd()) {
					Lox.error(line, "Unterminated block comment.");
				}

				// Closing /
				advance();
			} else {
				addToken(SLASH);
			}
			break;

		// meaningless characters, newlines, and whitespace
		case ' ':
		case '\r':
		case '\t':
			// Ignore whitespace.
			break;

		case '\n':
			line++;
			break;

		// string literals
		case '"':
			string();
			break;

		default:
			if (isDigit(c)) {
				number();
			} else if (isAlpha(c)) {
				identifier();
			} else {
				Lox.error(line, "Unexpected character.");
			}
			break;
		}
	}

	/**
	 * Scans an identifier.
	 */
	private void identifier() {
		while (isAlphaNumeric(peek())) {
			advance();
		}

		// See if the identifier is a reserved word.
		String lexeme = source.substring(start, current);
		TokenType type = keywords.getOrDefault(lexeme, IDENTIFIER);
		addToken(type);
	}

	/**
	 * Scans a number literal.
	 */
	private void number() {
		while (isDigit(peek())) {
			advance();
		}

		// Look for a fractional part.
		if (peek() == '.' && isDigit(peekNext())) {
			// Consume the "."
			advance();
		}

		while (isDigit(peek())) {
			advance();
		}

		addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
	}

	/**
	 * Scans a string literal.
	 */
	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			if (peek() == '\n') {
				line++;
			}
			advance();
		}

		// Unterminated string
		if (isAtEnd()) {
			Lox.error(line, "Unterminated string.");
		}

		// Closing "
		advance();

		// Trim the surrounding quotes.
		String value = source.substring(start + 1, current - 1);
		addToken(STRING, value);
	}

	/**
	 * If successful, increments the offset of the character under
	 * consideration.
	 * 
	 * @param expected
	 *            Some expected character.
	 * @return Whether the character under consideration equals some expected
	 *         character.
	 */
	private boolean match(char expected) {
		if (isAtEnd() || source.charAt(current) != expected) {
			return false;
		}

		current++;
		return true;
	}

	/**
	 * 
	 * @return If the offset of the character under consideration is greater
	 *         than or equal to the length of the sequence of characters
	 *         represented by some source code, then returns {@code '\0'}.
	 *         Otherwise, returns the char value at the offset of the current
	 *         character under consideration of some source code.
	 */
	private char peek() {
		return current >= source.length() ? '\0' : source.charAt(current);
	}

	/**
	 * 
	 * @return If the offset of the character under consideration plus one is
	 *         greater than or equal to the length of the sequence of characters
	 *         represented by some source code, then returns {@code '\0'}.
	 *         Otherwise, returns the char value at the offset of the current
	 *         character under consideration plus one of some source code.
	 */
	private char peekNext() {
		return current + 1 >= source.length() ? '\0' : source.charAt(current + 1);
	}

	/**
	 * No underscores.
	 * 
	 * @param c
	 *            Some character.
	 * @return Whether some character is an alphabetical character.
	 */
	private boolean isAlpha(char c) {
		return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
	}

	/**
	 * 
	 * @param c
	 *            Some character.
	 * @return Whether some character is an alphabetical character or some
	 *         character is between {@code '0'} and {@code '9'}.
	 */
	private boolean isAlphaNumeric(char c) {
		return isAlpha(c) || isDigit(c);
	}

	/**
	 * 
	 * @param c
	 *            Some character.
	 * @return Whether some character is between {@code '0'} and {@code '9'}.
	 */
	private boolean isDigit(char c) {
		return c >= '0' && c <= '9';
	}

	/**
	 * @return Whether the offset of the character under consideration is
	 *         greater than or equal to the length of the sequence of characters
	 *         represented by some source code.
	 */
	private boolean isAtEnd() {
		return current >= source.length();
	}

	/**
	 * @return The incremented offset of the character under consideration.
	 */
	private char advance() {
		current++;
		return source.charAt(current - 1);
	}

	/**
	 * Calls {@link #addToken(TokenType, Object)} with literal as {@code null}.
	 * 
	 * @param type
	 *            How the current token should be interpreted.
	 */
	private void addToken(TokenType type) {
		addToken(type, null);
	}

	/**
	 * Appends a new token to the ordered list of tokens generated from some
	 * source code whose lexeme is the substring in some source code between the
	 * offset of the first character in the current lexeme and the offset of the
	 * character under consideration and whose line is the current line count.
	 * 
	 * @param type
	 *            How the current token should be interpreted.
	 * @param literal
	 *            The value of the token.
	 */
	private void addToken(TokenType type, Object literal) {
		String lexeme = source.substring(start, current);
		tokens.add(new Token(type, lexeme, literal, line));
	}

}
