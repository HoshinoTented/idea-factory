package org.aya.tool.classfile

import jdk.internal.classfile.impl.SignaturesImpl
import kala.collection.immutable.ImmutableMap
import kala.collection.immutable.ImmutableSeq
import java.lang.classfile.MethodSignature
import java.lang.classfile.Signature
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.DirectMethodHandleDesc
import java.lang.constant.MethodTypeDesc
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

interface ClassRef {
  val descriptor: ClassDesc
  val polyCount: Int
}

/**
 * A reference to a method, which is designed to contain minimum information for method invocation
 */
interface MethodRef {
  val owner: ClassDesc
  val name: String
  val signature: MethodSignature
  val invokeKind: DirectMethodHandleDesc.Kind
  
  val descriptor: MethodTypeDesc
    get() = signature.erase()
  
  /**
   * Return the [MethodRef] before instantiation, null if this [MethodRef] haven't been/cannot be instantiated
   */
  val base: MethodRef? get() = (this as? ParameterizedSignature)?.base
  val isPoly: Boolean get() = signature.typeParameters().isNotEmpty()
}

data class DefaultMethodRef(
  override val owner: ClassDesc,
  override val name: String,
  override val invokeKind: DirectMethodHandleDesc.Kind,
  override val signature: MethodSignature,
) : MethodRef

fun MethodRef(
  owner: ClassDesc,
  name: String,
  parameter: ImmutableSeq<ClassDesc>,
  result: ClassDesc,
  invokeKind: DirectMethodHandleDesc.Kind,
): MethodRef {
  return DefaultMethodRef(
    owner, name, invokeKind,
    MethodSignature.of(MethodTypeDesc.of(result, *parameter.toArray(ClassDesc::class.java)))
  )
}