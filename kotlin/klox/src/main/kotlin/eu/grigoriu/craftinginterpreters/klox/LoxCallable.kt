package eu.grigoriu.craftinginterpreters.klox

interface LoxCallable {
    val arity: Int
    fun call(interpreter: Interpreter, arguments: List<Any?>): Any?
}
