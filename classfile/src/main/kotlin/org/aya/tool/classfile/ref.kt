package org.aya.tool.classfile

import kala.collection.immutable.ImmutableSeq
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.DirectMethodHandleDesc
import java.lang.constant.MethodTypeDesc

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
}

data class ParameterizedSignature(val base: MethodRef, val inst: ImmutableSeq<ClassDesc>) : MethodRef by base {
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
  val parameters: ImmutableSeq<Parameter>
  val returnType: Parameter
  val invokeKind: DirectMethodHandleDesc.Kind
  
  val descriptor: MethodTypeDesc
    get() {
      return MethodTypeDesc.of(returnType.erase(), parameters.map(Parameter::erase).asJava())
    }
}