package org.aya.tool.classfile

import java.lang.constant.ClassDesc

/**
 * A continuation about instruction generation.
 * When used as expression, i.e. arguments,
 * the continuation must push/load a value to the stack.
 *
 * ```
 * forall stack, {stack} CodeCont {exists value, value :: stack}
 * ```
 */
typealias CodeCont = CodeBuilderWrapper.() -> Unit

typealias MethodCodeCont = CodeBuilderWrapper.(ArgumentProvider) -> Unit
typealias MethodCodeCont0 = CodeBuilderWrapper.() -> Unit
typealias MethodCodeCont1 = CodeBuilderWrapper.(CodeBuilderWrapper.ExprCont) -> Unit
typealias MethodCodeCont2 = CodeBuilderWrapper.(CodeBuilderWrapper.ExprCont, CodeBuilderWrapper.ExprCont) -> Unit
typealias LambdaCodeCont = CodeBuilderWrapper.(LambdaArgumentProvider) -> Unit

/**
 * Returns the class name of this class descriptor. For example, `Foo` for `com.example.Foo` and `Bar` for `com.example.Foo$Baz$Bar`
 */
fun ClassDesc.className(): String {
  return displayName().substringAfterLast('$')
}