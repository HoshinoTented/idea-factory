package org.aya.tool.classfile

import kala.collection.immutable.ImmutableSeq
import java.lang.classfile.Signature
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.DirectMethodHandleDesc
import java.lang.constant.MethodTypeDesc

// TODO: remove this, use Signature
sealed interface Parameter {
  data class Exact(val type: ClassDesc) : Parameter
  data class Poly(val index: Int) : Parameter
  
  fun erase(): ClassDesc {
    return when (this) {
      is Exact -> type
      is Poly -> ConstantDescs.CD_Object
    }
  }
  
  fun instantiate(inst: ImmutableSeq<ClassDesc>): ClassDesc {
    return when (this) {
      is Exact -> type
      is Poly -> inst.getOrNull(index) ?: throw IndexOutOfBoundsException("instantiate")
    }
  }
  
  fun asSig(): Signature {
    return when (this) {
      is Exact -> Signature.of(this.type)
      is Poly -> Signature.TypeVarSig.of("T" + this.index)
    }
  }
}

data class ParameterizedSignature(override val base: MethodRef, val inst: ImmutableSeq<ClassDesc>) : MethodRef by base {
  override val returnType: Parameter = Parameter.Exact(base.returnType.instantiate(inst))
  override val parameters: ImmutableSeq<Parameter> = base.parameters.map { x ->
    Parameter.Exact(x.instantiate(inst))
  }
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
  val returnType: Parameter
  val parameters: ImmutableSeq<Parameter>
  val invokeKind: DirectMethodHandleDesc.Kind
  
  val descriptor: MethodTypeDesc
    get() {
      return MethodTypeDesc.of(returnType.erase(), parameters.map(Parameter::erase).asJava())
    }
  
  /**
   * Return the [MethodRef] before instantiation, null if this [MethodRef] haven't been/cannot be instantiated
   */
  val base: MethodRef? get() = (this as? ParameterizedSignature)?.base
  val isPoly: Boolean get() = parameters.anyMatch { it is Parameter.Poly }
}

data class DefaultMethodRef(
  override val owner: ClassDesc,
  override val invokeKind: DirectMethodHandleDesc.Kind,
  override val returnType: Parameter,
  override val name: String,
  override val parameters: ImmutableSeq<Parameter>,
) : MethodRef