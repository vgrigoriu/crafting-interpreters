package eu.grigoriu.craftinginterpreters.klox

class LoxFunction(
    private val declaration: Stmt.Function,
    private val closure: Environment,
    private val isInitializer: Boolean,
) : LoxCallable {
    override val arity: Int
        get() = declaration.params.size

    override fun call(interpreter: Interpreter, arguments: List<Any?>): Any? {
        val environment = Environment(closure)
        for (i in declaration.params.indices) {
            environment.define(declaration.params[i].lexeme, arguments[i])
        }

        try {
            interpreter.executeBlock(declaration.body, environment)
        } catch (returnValue: Return) {
            return if (isInitializer) {
                closure.getAt(0, "this")
            } else {
                returnValue.value
            }
        }

        if (isInitializer) {
            return closure.getAt(0, "this")
        }
        return null
    }

    internal fun bind(instance: LoxInstance): LoxFunction {
        val environment = Environment(closure)
        environment.define("this", instance)
        return LoxFunction(declaration, environment, isInitializer)
    }

    override fun toString(): String {
        return "<fn ${declaration.name.lexeme}>"
    }
}
