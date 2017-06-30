package lox;

import java.util.List;

abstract class Stmt {

	protected interface Visitor<R> {

		R visitBlockStmt(Block stmt);

		R visitExpressionStmt(Expression stmt);

		R visitPrintStmt(Print stmt);

		R visitVarStmt(Var stmt);

		R visitIfStmt(If stmt);

		R visitWhileStmt(While stmt);

	}

	protected static class Block extends Stmt {

		private final List<Stmt> statements;

		protected Block(List<Stmt> statements) {
			this.statements = statements;
		}

		public List<Stmt> statements() {
			return this.statements;
		}

		protected <R> R accept(Visitor<R> visitor) {
			return visitor.visitBlockStmt(this);
		}

	}

	protected static class Expression extends Stmt {

		private final Expr expression;

		protected Expression(Expr expression) {
			this.expression = expression;
		}

		public Expr expression() {
			return this.expression;
		}

		protected <R> R accept(Visitor<R> visitor) {
			return visitor.visitExpressionStmt(this);
		}

	}

	protected static class Print extends Stmt {

		private final Expr expression;

		protected Print(Expr expression) {
			this.expression = expression;
		}

		public Expr expression() {
			return this.expression;
		}

		protected <R> R accept(Visitor<R> visitor) {
			return visitor.visitPrintStmt(this);
		}

	}

	protected static class Var extends Stmt {

		private final Token name;

		private final Expr initializer;

		protected Var(Token name, Expr initializer) {
			this.name = name;
			this.initializer = initializer;
		}

		public Token name() {
			return this.name;
		}

		public Expr initializer() {
			return this.initializer;
		}

		protected <R> R accept(Visitor<R> visitor) {
			return visitor.visitVarStmt(this);
		}

	}

	protected static class If extends Stmt {

		private final Expr condition;

		private final Stmt thenBranch;

		private final Stmt elseBranch;

		protected If(Expr condition, Stmt thenBranch, Stmt elseBranch) {
			this.condition = condition;
			this.thenBranch = thenBranch;
			this.elseBranch = elseBranch;
		}

		public Expr condition() {
			return this.condition;
		}

		public Stmt thenBranch() {
			return this.thenBranch;
		}

		public Stmt elseBranch() {
			return this.elseBranch;
		}

		protected <R> R accept(Visitor<R> visitor) {
			return visitor.visitIfStmt(this);
		}

	}

	protected static class While extends Stmt {

		private final Expr condition;

		private final Stmt body;

		protected While(Expr condition, Stmt body) {
			this.condition = condition;
			this.body = body;
		}

		public Expr condition() {
			return this.condition;
		}

		public Stmt body() {
			return this.body;
		}

		protected <R> R accept(Visitor<R> visitor) {
			return visitor.visitWhileStmt(this);
		}

	}

	protected abstract <R> R accept(Visitor<R> visitor);

}
