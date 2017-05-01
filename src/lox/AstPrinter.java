package lox;

import lox.Expr.Binary;
import lox.Expr.Grouping;
import lox.Expr.Literal;
import lox.Expr.Ternary;
import lox.Expr.Unary;

public class AstPrinter implements Expr.Visitor<String> {

	public String print(Expr expr) {
		return expr.accept(this);
	}

	@Override
	public String visitLiteralExpr(Literal expr) {
		return expr.value().toString();
	}

	@Override
	public String visitUnaryExpr(Unary expr) {
		return parenthesize(expr.operator().lexeme(), expr.right());
	}

	@Override
	public String visitBinaryExpr(Binary expr) {
		return parenthesize(expr.operator().lexeme(), expr.left(), expr.right());
	}

	@Override
	public String visitTernaryExpr(Ternary expr) {
		return parenthesize(expr.leftOperator().lexeme() + expr.rightOperator().lexeme(), expr.left(), expr.middle(),
				expr.right());
	}

	@Override
	public String visitGroupingExpr(Grouping expr) {
		return parenthesize("group", expr.expression());
	}

	private String parenthesize(String name, Expr... exprs) {
		StringBuilder builder = new StringBuilder();

		builder.append("(").append(name);
		for (Expr expr : exprs) {
			builder.append(" ").append(expr.accept(this));
		}
		builder.append(")");

		return builder.toString();
	}

}
