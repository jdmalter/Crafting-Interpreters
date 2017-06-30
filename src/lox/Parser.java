package lox;

import java.util.ArrayList;
import java.util.Arrays;
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
	 * @return The ordered list of statements derived from the ordered list of
	 *         tokens generated from some source code.
	 */
	protected List<Stmt> parse() {
		List<Stmt> statements = new ArrayList<Stmt>();

		while (!isAtEnd()) {
			statements.add(declaration());
		}

		return statements;
	}

	/**
	 * 
	 * @return A tree of comma rules.
	 */
	private Expr expression() {
		return assignment();
	}

	/**
	 * 
	 * @return A var token followed
	 */
	private Stmt declaration() {
		try {
			if (match(TokenType.VAR)) {
				return varDeclaration();
			}

			return statement();
		} catch (ParseError error) {
			synchronize();
			return null;
		}
	}

	/**
	 * 
	 * @return Either a print, block, or expression statement.
	 */
	private Stmt statement() {
		if (match(TokenType.FOR)) {
			return forStatement();
		}
		if (match(TokenType.IF)) {
			return ifStatement();
		}
		if (match(TokenType.PRINT)) {
			return printStatement();
		}
		if (match(TokenType.WHILE)) {
			return whileStatement();
		}
		if (match(TokenType.LEFT_BRACE)) {
			return new Stmt.Block(block());
		}

		return expressionStatement();
	}

	private Stmt forStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

		Stmt initializer;
		if (match(TokenType.SEMICOLON)) {
			initializer = null;
		} else if (match(TokenType.VAR)) {
			initializer = varDeclaration();
		} else {
			initializer = expressionStatement();
		}

		Expr condition = null;
		if (!check(TokenType.SEMICOLON)) {
			condition = expression();
		}
		consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

		Expr increment = null;
		if (!check(TokenType.RIGHT_PAREN)) {
			increment = expression();
		}
		consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");
		Stmt body = statement();

		if (increment != null) {
			body = new Stmt.Block(Arrays.asList(body, new Stmt.Expression(increment)));
		}

		if (condition == null) {
			condition = new Expr.Literal(true);
		}
		body = new Stmt.While(condition, body);

		if (initializer != null) {
			body = new Stmt.Block(Arrays.asList(initializer, body));
		}

		return body;
	}

	private Stmt ifStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");

		Stmt thenBranch = statement();
		Stmt elseBranch = null;
		if (match(TokenType.ELSE)) {
			elseBranch = statement();
		}

		return new Stmt.If(condition, thenBranch, elseBranch);
	}

	/**
	 * 
	 * @return A variable statement.
	 */
	private Stmt varDeclaration() {
		Token name = consume(TokenType.IDENTIFIER, "Expect varaible name.");

		Expr initializer = null;
		if (match(TokenType.EQUAL)) {
			initializer = expression();
		}

		consume(TokenType.SEMICOLON, "Expect ',' after variable declaration.");
		return new Stmt.Var(name, initializer);
	}

	private Stmt whileStatement() {
		consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
		Expr condition = expression();
		consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
		Stmt body = statement();

		return new Stmt.While(condition, body);
	}

	/**
	 * 
	 * @return A tree of comma rules started with a print statement and followed
	 *         by one semicolon.
	 */
	private Stmt printStatement() {
		Expr value = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after value.");
		return new Stmt.Print(value);
	}

	/**
	 * 
	 * @return A tree of comma rules followed by one semicolon.
	 */
	private Stmt expressionStatement() {
		Expr expr = expression();
		consume(TokenType.SEMICOLON, "Expect ';' after expression.");
		return new Stmt.Expression(expr);
	}

	/**
	 * 
	 * @return A list of statements.
	 */
	private List<Stmt> block() {
		List<Stmt> statements = new ArrayList<Stmt>();

		while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
			statements.add(declaration());
		}

		consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
		return statements;
	}

	/**
	 * 
	 * @return A tree where the left value is a variable and the right value is
	 *         a value possibly followed by more assignments.
	 */
	private Expr assignment() {
		Expr expr = or();

		if (match(TokenType.EQUAL)) {
			Token equals = previous();
			Expr value = assignment();

			if (expr instanceof Expr.Variable) {
				Token name = ((Expr.Variable) expr).name();
				return new Expr.Assign(name, value);
			}

			error(equals, "Invalid assignment target.");
		}

		return expr;
	}

	private Expr or() {
		Expr expr = and();

		while (match(TokenType.OR)) {
			Token operator = previous();
			Expr right = and();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	private Expr and() {
		Expr expr = equality();

		while (match(TokenType.AND)) {
			Token operator = previous();
			Expr right = conditional();
			expr = new Expr.Logical(expr, operator, right);
		}

		return expr;
	}

	/**
	 * 
	 * @return A ternary expression with a condition and trees of expression
	 *         rules whether the condition evalutes to true or false.
	 */
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

		if (match(TokenType.IDENTIFIER)) {
			return new Expr.Variable(previous());
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
	 *            An error describing string.
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
