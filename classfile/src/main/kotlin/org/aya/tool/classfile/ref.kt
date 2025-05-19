package org.aya.tool.classfile

import kala.collection.immutable.ImmutableMap
import org.aya.tool.classfile.data.MethodRef
import java.lang.classfile.MethodSignature
import java.lang.classfile.Signature
import java.lang.constant.ClassDesc
import kotlin.jvm.optionals.getOrNull

/**
 * @param inst the value should be class or interface
 */
data class ParameterizedSignature(override val base: MethodRef, val inst: ImmutableMap<String, ClassDesc>) :
  MethodRef by base {
  private fun Signature.TypeArg.instantiate(): Signature.TypeArg {
    return when (this) {
      is Signature.TypeArg.Bounded -> Signature.TypeArg.of(boundType().instantiate() as Signature.RefTypeSig)
      is Signature.TypeArg.Unbounded -> this
    }
  }
  
  private fun Signature.instantiate(): Signature {
    return when (this) {
      is Signature.ArrayTypeSig -> Signature.ArrayTypeSig.of(arrayDepth(), componentSignature().instantiate())
      
      is Signature.ClassTypeSig ->
        Signature.ClassTypeSig.of(outerType().getOrNull(), className(), *(typeArgs().map { it.instantiate() }.toTypedArray()))
      
      is Signature.TypeVarSig -> Signature.of(inst.get(this.identifier()))
      else -> this
    }
  }
  
  override val signature: MethodSignature = MethodSignature.of(
    emptyList(), base.signature.throwableSignatures(), base.signature.result().instantiate(),
    *base.signature.arguments().map { it.instantiate() }.toTypedArray()
  )
}

