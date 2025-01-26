package org.aya.tool.classfile

import kala.collection.immutable.ImmutableSeq
import org.aya.tool.classfile.ArgumentProvider.Companion.mkLoad
import java.lang.classfile.TypeKind
import java.lang.constant.ClassDesc
import java.lang.constant.MethodTypeDesc

interface ArgumentProvider {
  companion object {
    fun mkLoad(type: ClassDesc, slot: Int): CodeBuilderWrapper.ExprCont {
      return CodeBuilderWrapper.ExprCont(type) {
        builder.loadLocal(TypeKind.fromDescriptor(type.descriptorString()), slot)
      }
    }
  }
  
  fun arg(nth: Int): CodeBuilderWrapper.ExprCont
}

class DefaultArgumentProvider(val signature: MethodTypeDesc, val hasThis: Boolean) : ArgumentProvider {
  private fun computeArg(nth: Int): Int {
    return (if (hasThis) 1 else 0) + nth
  }
  
  override fun arg(nth: Int): CodeBuilderWrapper.ExprCont {
    assert(nth < signature.parameterCount())
    val type = signature.parameterType(nth)
    val actualSlot = computeArg(nth)
    return mkLoad(type, actualSlot)
  }
}

class LambdaArgumentProvider(
  val captures: ImmutableSeq<ClassDesc>,
  val signature: MethodTypeDesc
) : ArgumentProvider {
  val captureCount = captures.size()
  
  fun capture(nth: Int): CodeBuilderWrapper.ExprCont {
    assert(nth < captureCount)
    val type = captures.get(nth)
    return mkLoad(type, nth)
  }
  
  override fun arg(nth: Int): CodeBuilderWrapper.ExprCont {
    assert(captureCount <= nth && nth < captureCount + signature.parameterCount())
    val actualSlot = nth - captureCount
    val type = signature.parameterType(actualSlot)
    return mkLoad(type, actualSlot)
  }
}