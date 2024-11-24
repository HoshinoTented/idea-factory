package org.aya.tool.classfile

typealias MethodCodeCont = CodeBuilderWrapper.(ArgumentProvider) -> Unit
typealias MethodCodeCont0 = CodeBuilderWrapper.() -> Unit
typealias MethodCodeCont1 = CodeBuilderWrapper.(CodeBuilderWrapper.ExprCont) -> Unit
typealias MethodCodeCont2 = CodeBuilderWrapper.(CodeBuilderWrapper.ExprCont, CodeBuilderWrapper.ExprCont) -> Unit
typealias LambdaCodeCont = CodeBuilderWrapper.(LambdaArgumentProvider) -> Unit