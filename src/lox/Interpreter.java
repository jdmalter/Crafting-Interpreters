package lox;

import java.util.List;
import java.util.Objects;

import lox.Expr.Binary;
import lox.Expr.Grouping;
import lox.Expr.Literal;
import lox.Expr.Logical;
import lox.Expr.Ternary;
import lox.Expr.Unary;
import lox.Stmt.If;
import lox.Stmt.While;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

	/** Maps identifiers to values. */
	private Environment environment = new Environment();

	/**
	 * 
	 * @param statements
	 *            an ordered list of statements
	 */
	protected void interpret(List<Stmt> statements) {
		try {
			statements.forEach(this::execute);
		} catch (RuntimeError error) {
			Lox.runtimeError(error);
		}
	}

	/**
	 * Evaluates some provided expression.
	 * 
	 * @param expr
	 *            some provided expression
	 * @return What value some provided expression evaluates to.
	 */
	private Object evaluate(Expr expr) {
		return expr.accept(this);
	}

	/**
	 * Executes some provided statement.
	 * 
	 * @param stmt
	 *            some provided statement
	 */
	private void execute(Stmt stmt) {
		stmt.accept(this);
	}

	/**
	 * 
	 * @param statements
	 *            some provided statements
	 * @param environment
	 *            Maps identifiers to values.
	 */
	void executeBlock(List<Stmt> statements, Environment environment) {
		Environment previous = this.environment;
		try {
			this.environment = environment;

			statements.forEach(this::execute);
		} finally {
			this.environment = previous;
		}
	}

	@Override
	public Void visitBlockStmt(Stmt.Block stmt) {
		executeBlock(stmt.statements(), new Environment(environment));
		return null;
	}

	@Override
	public Void visitExpressionStmt(Stmt.Expression stmt) {
		evaluate(stmt.expression());
		return null;
	}

	@Override
	public Void visitIfStmt(If stmt) {
		if (isTrue(evaluate(stmt.condition()))) {
			execute(stmt.thenBranch());
		} else if (stmt.elseBranch() != null) {
			execute(stmt.elseBranch());
		}
		return null;
	}

	@Override
	public Void visitPrintStmt(Stmt.Print stmt) {
		Object value = evaluate(stmt.expression());
		System.out.println(stringify(value));
		return null;
	}

	@Override
	public Void visitVarStmt(Stmt.Var stmt) {
		Object value = null;
		if (stmt.initializer() != null) {
			value = evaluate(stmt.initializer());
		}

		environment.define(stmt.name().lexeme(), value);
		return null;
	}

	@Override
	public Void visitWhileStmt(While stmt) {
		while (isTrue(evaluate(stmt.condition()))) {
			execute(stmt.body());
		}
		return null;
	}

	public Object visitAssignExpr(Expr.Assign expr) {
		Object value = evaluate(expr.value());

		environment.assign(expr.name(), value);
		return value;
	}

	@Override
	public Object visitLiteralExpr(Literal expr) {
		return expr.value();
	}

	@Override
	public Object visitUnaryExpr(Unary expr) {
		Object right = evaluate(expr.right());

		switch (expr.operator().type()) {
		case BANG:
			return !isTrue(right);
		case MINUS:
			checkNumberOperand(expr.operator(), right);
			return -(double) right;
		default:
			break;
		}

		// Unreachable.
		return null;
	}

	@Override
	public Object visitVariableExpr(Expr.Variable expr) {
		return environment.get(expr.name());
	}

	/**
	 * 
	 * @param object
	 *            Some provided object.
	 * @return Whether some provided object is not null and not a false boolean.
	 */
	private boolean isTrue(Object object) {
		return object != null && (!(object instanceof Boolean) || (boolean) object);
	}

	/**
	 * 
	 * @param object
	 *            some provided object
	 * @return a string representation of some provided object.
	 */
	private String stringify(Object object) {
		if (object == null) {
			return "nil";
		}

		// Hack. Work around Java adding ".0" to integer-valued doubles.
		if (object instanceof Double) {
			String text = object.toString();
			if (text.endsWith(".0")) {
				text = text.substring(0, text.length() - 2);
			}
			return text;
		}

		return object.toString();
	}

	@Override
	public Object visitBinaryExpr(Binary expr) {
		Object left = evaluate(expr.left());
		Object right = evaluate(expr.right());

		switch (expr.operator().type()) {
		// Equality operators
		case BANG_EQUAL:
			return !Objects.deepEquals(left, right);
		case EQUAL_EQUAL:
			return Objects.deepEquals(left, right);

		// Comparison operators
		case GREATER:
			checkNumberOperands(expr.operator(), left, right);
			return (double) left > (double) right;
		case GREATER_EQUAL:
			checkNumberOperands(expr.operator(), left, right);
			return (double) left >= (double) right;
		case LESS:
			checkNumberOperands(expr.operator(), left, right);
			return (double) left < (double) right;
		case LESS_EQUAL:
			checkNumberOperands(expr.operator(), left, right);
			return (double) left <= (double) right;

		// Arithmetic operators
		case MINUS:
			checkNumberOperands(expr.operator(), left, right);
			return (double) left - (double) right;
		case PLUS:
			if (left instanceof Double && right instanceof Double) {
				return (double) left + (double) right;
			}

			if (left instanceof String || right instanceof String) {
				return stringify(left) + stringify(right);
			}

			throw new RuntimeError(expr.operator(), "Operands must be two numbers or two strings.");
		case SLASH:
			checkNumberOperands(expr.operator(), left, right);

			if (+0.0d == (double) right || -0.0d == (double) right) {
				throw new RuntimeError(expr.operator(), "Cannot divide by zero!");
			}

			return (double) left / (double) right;
		case STAR:
			checkNumberOperands(expr.operator(), left, right);
			return (double) left * (double) right;
		default:
			break;
		}

		// Unreachable.
		return null;
	}

	private void checkNumberOperand(Token operator, Object operand) {
		if (operand instanceof Double) {
			return;
		}

		throw new RuntimeError(operator, "Operand must be a number");
	}

	private void checkNumberOperands(Token operator, Object left, Object right) {
		if (left instanceof Double && right instanceof Double) {
			return;
		}

		throw new RuntimeError(operator, "Operands must be numbers.");
	}

	@Override
	public Object visitTernaryExpr(Ternary expr) {
		Object left = evaluate(expr.left());

		switch (expr.leftOperator().type()) {
		case QUESTION:
			return isTrue(left) ? evaluate(expr.middle()) : evaluate(expr.right());
		default:
			break;
		}

		// Unreachable.
		return null;
	}

	@Override
	public Object visitGroupingExpr(Grouping expr) {
		return evaluate(expr.expression());
	}

	@Override
	public Object visitLogicalExpr(Logical expr) {
		Object left = evaluate(expr.left());

		if (expr.operator().type() == TokenType.OR) {
			if (isTrue(left)) {
				return left;
			}
		} else {
			if (!isTrue(left)) {
				return left;
			}
		}

		return evaluate(expr.right());
	}

}
