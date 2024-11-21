package org.aya.tool.classfile

import kala.collection.immutable.ImmutableSeq
import java.lang.classfile.TypeKind
import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc

open class ArgumentProvider(val signature: MethodTypeDesc, private val hasThis: Boolean) {
  constructor(data: MethodData) : this(data.signature, !data.isStatic)
  
  private fun computeArg(nth: Int): Int {
    return (if (hasThis) 1 else 0) + nth
  }
  
  open fun arg(nth: Int): CodeBuilderWrapper.ExprCont {
    val type = signature.parameterType(nth)
    val actualSlot = computeArg(nth)
    return CodeBuilderWrapper.ExprCont(signature.parameterType(nth)) {
      builder.loadInstruction(TypeKind.fromDescriptor(type.descriptorString()), actualSlot)
    }
  }
}

class LambdaArgumentProvider(
  captures: ImmutableSeq<ClassDesc>,
  signature: MethodTypeDesc
) : ArgumentProvider(
  MethodTypeDesc.of(signature.returnType(), captures.view().appendedAll(signature.parameterList()).toList()),
  false
) {
  val captureCount = captures.size()
  
  fun capture(nth: Int): CodeBuilderWrapper.ExprCont {
    return super.arg(nth)
  }
  
  override fun arg(nth: Int): CodeBuilderWrapper.ExprCont {
    return super.arg(captureCount + nth)
  }
}