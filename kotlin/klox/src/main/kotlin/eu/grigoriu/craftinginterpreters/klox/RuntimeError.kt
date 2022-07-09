package eu.grigoriu.craftinginterpreters.klox

class RuntimeError(val token: Token, message: String) : RuntimeException(message)
