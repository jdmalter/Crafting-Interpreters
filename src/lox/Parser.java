package lox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Parser {

	@SuppressWarnings("serial")
	private static class ParseError extends RuntimeException {

	}

	/** The ordered list of tokens generated from some source code. */
	private final List<Token> tokens = new ArrayList<Token>();
	/** The offset of the token under consideration. */
	private int current = 0;

	/**
	 * 
	 * @param tokens
	 *            The ordered list of tokens generated from some source code.
	 */
	public Parser(List<Token> tokens) {
		this.tokens.addAll(tokens);
	}

	/**
	 * 
	 * @return An expresssion derived from the ordered list of tokens generated
	 *         from some source code.
	 */
	protected Expr parse() {
		try {
			return expression();
		} catch (ParseError error) {
			return null;
		}
	}

	/**
	 * 
	 * @return A tree of comma rules.
	 */
	private Expr expression() {
		return conditional();
	}

	private Expr conditional() {
		return ternary(this::comma, TokenType.QUESTION, TokenType.COLON);
	}

	/**
	 * 
	 * @return A tree of expression rules.
	 */
	private Expr comma() {
		return binary(this::equality, TokenType.COMMA);
	}

	/**
	 * 
	 * @return A tree of comparison rules.
	 */
	private Expr equality() {
		return binary(this::comparison, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL);
	}

	/**
	 * 
	 * @return A tree of term rules.
	 */
	private Expr comparison() {
		return binary(this::term, TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL);
	}

	/**
	 * 
	 * @return A tree of factor rules.
	 */
	private Expr term() {
		return binary(this::factor, TokenType.PLUS, TokenType.MINUS);
	}

	/**
	 * 
	 * @return A tree of unary rules.
	 */
	private Expr factor() {
		return binary(this::unary, TokenType.SLASH, TokenType.STAR);
	}

	/**
	 * 
	 * @return A list of primary rules.
	 */
	private Expr unary() {
		if (match(TokenType.BANG, TokenType.MINUS)) {
			Token operator = previous();
			Expr right = unary();
			return new Expr.Unary(operator, right);
		}

		return primary();
	}

	/**
	 * 
	 * @return A literal.
	 */
	private Expr primary() {
		if (match(TokenType.FALSE)) {
			return new Expr.Literal(false);
		}
		if (match(TokenType.TRUE)) {
			return new Expr.Literal(true);
		}
		if (match(TokenType.NIL)) {
			return new Expr.Literal(null);
		}

		if (match(TokenType.NUMBER, TokenType.STRING)) {
			return new Expr.Literal(previous().literal());
		}

		if (match(TokenType.LEFT_PAREN)) {
			Expr expr = expression();
			consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
			return new Expr.Grouping(expr);
		}

		throw error(peek(), "Expected expression.");
	}

	/**
	 * 
	 * @param operands
	 *            Supplier of expression operands.
	 * @param type
	 *            Some token type to match.
	 * @param types
	 *            Some token types to match.
	 * @return A ternary expression.
	 */
	private Expr ternary(Supplier<Expr> operands, TokenType type, TokenType... types) {
		Expr expr = operands.get();

		if (match(type)) {
			Token leftOperator = previous();
			Expr middle = operands.get();
			if (match(types)) {
				Token rightOperator = previous();
				Expr right = operands.get();
				expr = new Expr.Ternary(expr, leftOperator, middle, rightOperator, right);
			}
		}

		return expr;
	}

	/**
	 * 
	 * @param operands
	 *            Supplier of expression operands.
	 * @param types
	 *            Some token types to match.
	 * @return A tree of supplied expression operands.
	 */
	private Expr binary(Supplier<Expr> operands, TokenType... types) {
		Expr expr = operands.get();

		while (match(types)) {
			Token operator = previous();
			Expr right = operands.get();
			expr = new Expr.Binary(expr, operator, right);
		}

		return expr;
	}

	/**
	 * If successful, incremenets the offset of the token under consideration.
	 * 
	 * @param types
	 *            Some token types.
	 * @return Whether any token type in types is not EOF and equals the token
	 *         type of the token under consideration is EOF the token under
	 *         consideration.
	 */
	private boolean match(TokenType... types) {
		for (TokenType type : types) {
			if (check(type)) {
				advance();
				return true;
			}
		}

		return false;
	}

	/**
	 * 
	 * @param type
	 *            Some token type.
	 * @param message
	 * @return If the token under consideration is not EOF some token type and
	 *         equals the token type of the token under consideration is EOF the
	 *         token under consideration, then returns the token under
	 *         consideration.
	 */
	private Token consume(TokenType type, String message) {
		if (check(type)) {
			return advance();
		}

		throw error(peek(), message);
	}

	/**
	 * 
	 * @param type
	 *            Some token type.
	 * @return Whether the token under consideration is not EOF some token type
	 *         and equals the token type of the token under consideration is EOF
	 *         the token under consideration.
	 */
	private boolean check(TokenType type) {
		return !isAtEnd() && peek().type() == type;
	}

	/**
	 * If the token under consideration is not EOF, incremenets the offset of
	 * the token under consideration.
	 * 
	 * @return The token previously under consideration.
	 */
	private Token advance() {
		if (!isAtEnd()) {
			current++;
		}

		return previous();
	}

	/**
	 * 
	 * @return Whether the token under consideration is EOF.
	 */
	private boolean isAtEnd() {
		return peek().type() == TokenType.EOF;
	}

	/**
	 * 
	 * @return The token under consideration.
	 */
	private Token peek() {
		return tokens.get(current);
	}

	/**
	 * 
	 * @return The token previously under consideration.
	 */
	private Token previous() {
		return tokens.get(current - 1);
	}

	/**
	 * 
	 * @param token
	 *            Some provided token.
	 * @param message
	 *            An error describing string.
	 * @return
	 */
	private ParseError error(Token token, String message) {
		Lox.error(token, message);
		return new ParseError();
	}

	/**
	 * Discard tokens until the beginning of the next statement.
	 */
	private void synchronize() {
		advance();

		while (!isAtEnd()) {
			if (previous().type() == TokenType.SEMICOLON) {
				return;
			}

			switch (peek().type()) {
			case CLASS:
			case FUN:
			case VAR:
			case FOR:
			case IF:
			case WHILE:
			case PRINT:
			case RETURN:
				return;
			default:
				break;
			}

			advance();
		}
	}

}
