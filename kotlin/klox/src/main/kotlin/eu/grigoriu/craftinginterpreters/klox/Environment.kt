package eu.grigoriu.craftinginterpreters.klox

class Environment {
    private val values = mutableMapOf<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun assign(name: Token, value: Any?) {
        if (!values.containsKey(name.lexeme)) {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
        values[name.lexeme] = value
    }

    fun get(name: Token): Any? {
        if (!values.containsKey(name.lexeme)) {
            throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
        }
        return values[name.lexeme]
    }
}
