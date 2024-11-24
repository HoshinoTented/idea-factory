package org.aya.tool.classfile

/**
 * A continuation about instruction generation.
 * When used as expression, i.e. arguments,
 * the continuation must push/load a value to the stack.
 */
typealias CodeCont = CodeBuilderWrapper.() -> Unit

typealias MethodCodeCont = CodeBuilderWrapper.(ArgumentProvider) -> Unit
typealias MethodCodeCont0 = CodeBuilderWrapper.() -> Unit
typealias MethodCodeCont1 = CodeBuilderWrapper.(CodeBuilderWrapper.ExprCont) -> Unit
typealias MethodCodeCont2 = CodeBuilderWrapper.(CodeBuilderWrapper.ExprCont, CodeBuilderWrapper.ExprCont) -> Unit
typealias LambdaCodeCont = CodeBuilderWrapper.(LambdaArgumentProvider) -> Unit