package lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Lox {

	/** An instance of Lox's interpreter. */
	private static final Interpreter interpreter = new Interpreter();

	/** Whether an error was reported. */
	private static boolean hadError = false;
	/** Whether a runtime error was reported. */
	private static boolean hadRuntimeError = false;

	/**
	 * Starts the Lox interpreter.
	 * 
	 * @param args
	 *            the path string
	 * @throws IOException
	 *             if an I/O error occurs reading from the byte array containing
	 *             the bytes read from the file given by the path
	 */
	public static void main(String[] args) throws IOException {
		if (args.length > 1) {
			System.out.println("Useage: jlox [script]");
		} else if (args.length == 1) {
			runFile(args[0]);
		} else {
			runPrompt();
		}
	}

	/**
	 * Reads file and executes its.
	 * 
	 * @param path
	 *            the path string
	 * @throws IOException
	 *             if an I/O error occurs reading from the byte array containing
	 *             the bytes read from the file given by the path
	 */
	private static void runFile(String path) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(path));
		run(new String(bytes, Charset.defaultCharset()));

		if (hadError) {
			System.exit(65);
		}
		if (hadRuntimeError) {
			System.exit(70);
		}
	}

	/**
	 * Run interative interpreter.
	 * 
	 * @throws IOException
	 *             If an I/O error occurs while reading lines from the
	 *             "standard" input stream
	 */
	private static void runPrompt() throws IOException {
		InputStreamReader input = new InputStreamReader(System.in);
		BufferedReader reader = new BufferedReader(input);

		for (;;) {
			System.out.print("> ");
			run(reader.readLine());
			hadError = false;
		}
	}

	/**
	 * 
	 * @param source
	 *            Some source code.
	 */
	private static void run(String source) {
		Scanner scanner = new Scanner(source);
		List<Token> tokens = scanner.scanTokens();
		Parser parser = new Parser(tokens);
		List<Stmt> statements = parser.parse();

		// Stop if there was a syntax error
		if (hadError) {
			return;
		}

		interpreter.interpret(statements);
	}

	/**
	 * Reports an error.
	 * 
	 * @param line
	 *            What line contains some syntax error.
	 * @param message
	 *            An error describing string.
	 */
	protected static void error(int line, String message) {
		report(line, "", message);
	}

	/**
	 * Reports an error.
	 * 
	 * @param line
	 *            What line contains some syntax error.
	 * @param where
	 *            A location describing string.
	 * @param message
	 *            An error describing string.
	 */
	private static void report(int line, String where, String message) {
		System.err.println("[line " + line + "] Error" + where + ": " + message);
		hadError = true;
	}

	/**
	 * Reports an error on a token.
	 * 
	 * @param token
	 *            Some provided token.
	 * @param message
	 *            An error describing string.
	 */
	protected static void error(Token token, String message) {
		if (token.type() == TokenType.EOF) {
			report(token.line(), " at end", message);
		} else {
			report(token.line(), " at '" + token.lexeme() + "'", message);
		}
	}

	protected static void runtimeError(RuntimeError error) {
		System.err.println(error.getMessage() + "\n[line " + error.token().line() + "]");
		hadRuntimeError = true;
	}

}
