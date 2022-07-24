package eu.grigoriu.craftinginterpreters.klox

import java.util.Stack

abstract class Resolver(private val interpreter: Interpreter) : Expr.Visitor<Unit>, Stmt.Visitor<Unit> {
    private val scopes = Stack<MutableMap<String, Boolean>>()

    override fun visitBlockStmt(stmt: Stmt.Block) {
        beginScope()
        resolve(stmt.statements)
        endScope()
    }

    override fun visitVarStmt(stmt: Stmt.Var) {
        declare(stmt.name)
        if (stmt.initializer != null) {
            resolve(stmt.initializer)
        }
        define(stmt.name)
    }

    private fun beginScope() {
        scopes.push(HashMap())
    }

    private fun endScope() {
        scopes.pop()
    }

    private fun resolve(statements: List<Stmt?>) {
        for (statement in statements.filterNotNull()) {
            resolve(statement)
        }
    }

    private fun resolve(stmt: Stmt) {
        stmt.accept(this)
    }

    private fun resolve(expr: Expr) {
        expr.accept(this)
    }

    private fun declare(name: Token) {
        if (scopes.empty()) {
            return
        }
        // Variable not ready yet.
        scopes.peek()[name.lexeme] = false
    }

    private fun define(name: Token) {
        if (scopes.empty()) {
            return
        }
        // Variable ready.
        scopes.peek()[name.lexeme] = true
    }
}