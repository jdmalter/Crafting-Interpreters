package tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Useage: generate_ast <output directory>");
			System.exit(1);
		}

		String outputDir = args[0];
		defineAst(outputDir, "Expr",
				Arrays.asList("Literal -> Object value", "Unary -> Token operator, Expr right",
						"Binary -> Expr left, Token operator, Expr right",
						"Ternary -> Expr left, Token leftOperator, Expr middle, Token rightOperator, Expr right",
						"Grouping -> Expr expression"));
	}

	private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
		String path = outputDir + "/" + baseName + ".java";
		PrintWriter writer = new PrintWriter(path, "UTF-8");

		writer.println("package lox;");
		writer.println();
		writer.println("abstract class " + baseName + " {");
		writer.println();

		defineVisitor(writer, baseName, types);
		writer.println();

		for (String type : types) {
			String className = type.split("->")[0].trim();
			String fields = type.split("->")[1].trim();
			defineType(writer, baseName, className, fields);
			writer.println();
		}

		writer.println("	protected abstract <R> R accept(Visitor<R> visitor);");
		writer.println();

		writer.println("}");
		writer.close();
	}

	private static void defineType(PrintWriter writer, String baseName, String className, String fieldList) {
		writer.println("	protected static class " + className + " extends " + baseName + " {");
		writer.println();

		// Fields
		String[] fields = fieldList.split(", ");
		for (String field : fields) {
			writer.println("		private final " + field + ";");
			writer.println();
		}

		// Constructor
		writer.println("		protected " + className + "(" + fieldList + ") {");

		// Store parameters in fields
		for (String field : fields) {
			String name = field.split(" ")[1];
			writer.println("			this." + name + " = " + name + ";");
		}

		// Close constructor
		writer.println("		}");
		writer.println();

		// Getters
		for (String field : fields) {
			String name = field.split(" ")[1];
			writer.println("		public " + field + "() {");
			writer.println("			return this." + name + ";");
			writer.println("		}");
			writer.println();
		}

		// Visitor pattern
		writer.println("		protected <R> R accept(Visitor<R> visitor) {");
		writer.println("			return visitor.visit" + className + baseName + "(this);");
		writer.println("		}");
		writer.println();

		// Close class
		writer.println("	}");
	}

	private static void defineVisitor(PrintWriter writer, String baseName, List<String> types) {
		writer.println("	protected interface Visitor<R> {");
		writer.println();

		for (String type : types) {
			String typeName = type.split("->")[0].trim();
			writer.println(
					"		R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
			writer.println();
		}

		writer.println("	}");
	}

}
