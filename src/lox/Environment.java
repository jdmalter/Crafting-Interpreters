package lox;

import java.util.HashMap;
import java.util.Map;

class Environment {

	/** An immediately enclosing environment with its own scope. */
	protected final Environment enclosing;
	/** A map of identifier lexemes to values. */
	private final Map<String, Object> values = new HashMap<String, Object>();

	Environment() {
		this.enclosing = null;
	}

	Environment(Environment enclosing) {
		this.enclosing = enclosing;
	}

	/**
	 * 
	 * @param name
	 *            name with which the specified value evaluates to
	 * @param value
	 *            value to be evaluated by the specified name
	 */
	void define(String name, Object value) {
		values.put(name, value);
	}

	/**
	 * 
	 * @param name
	 *            name with which evaluates to some value
	 * @return the value which the specified name evaluates to
	 */
	Object get(Token name) {
		if (values.containsKey(name.lexeme())) {
			return values.get(name.lexeme());
		}

		if (enclosing != null) {
			return enclosing.get(name);
		}

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme() + "'.");
	}

	/**
	 * 
	 * @param name
	 *            name with which evaluates to some value
	 * @param value
	 *            value to be evaluated by the specified name
	 */
	void assign(Token name, Object value) {
		if (values.containsKey(name.lexeme())) {
			values.put(name.lexeme(), value);
			return;
		}

		if (enclosing != null) {
			enclosing.assign(name, value);
			return;
		}

		throw new RuntimeError(name, "Undefined variable '" + name.lexeme() + "'.");
	}

}
