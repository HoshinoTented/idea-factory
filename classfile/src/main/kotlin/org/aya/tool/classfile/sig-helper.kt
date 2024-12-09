package org.aya.tool.classfile

import jdk.internal.classfile.impl.SignaturesImpl
import java.lang.classfile.MethodSignature
import java.lang.classfile.Signature
import java.lang.constant.ClassDesc
import kala.collection.mutable.MutableList
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc

class SignatureBuilder(
  private val buffer: MutableList<Signature>,
  private var polyCount: Int,
  private val typeVars: MutableList<Signature.TypeParam>,
) {
  fun typeVar(classBound: ClassDesc?, vararg interfaceBounds: ClassDesc): Signature.TypeVarSig {
    val param = Signature.TypeParam.of(
      "T$polyCount",
      classBound?.let(Signature.ClassTypeSig::of),
      *Array(interfaceBounds.size) { i ->
        Signature.ClassTypeSig.of(interfaceBounds[i])
      }
    )
    
    return Signature.TypeVarSig.of(param.identifier())
  }
  
  operator fun ClassDesc.unaryPlus() {
    buffer.append(Signature.of(this))
  }
  
  operator fun Signature.unaryPlus() {
    buffer.append(this)
  }
  
  fun ret(exact: ClassDesc): MethodSignature {
    return ret(Signature.of(exact))
  }
  
  fun ret(p: Signature): MethodSignature {
    return MethodSignature.of(
      typeVars.asJava(), emptyList(), p,
      *buffer.toArray(Signature::class.java)
    )
  }
}

inline fun buildSignature(builder: SignatureBuilder.() -> Unit): SignatureBuilder {
  return SignatureBuilder(MutableList.create(), 0, MutableList.create()).apply(builder)
}

fun Signature.erase(): ClassDesc {
  return when (this) {
    is SignaturesImpl.BaseTypeSigImpl -> ClassDesc.of(this.signatureString())
    is SignaturesImpl.ArrayTypeSigImpl -> componentSignature().erase().arrayType(arrayDepth)
    is SignaturesImpl.ClassTypeSigImpl -> ClassDesc.ofInternalName(className)
    is SignaturesImpl.TypeVarSigImpl -> ConstantDescs.CD_Object
  }
}

fun MethodSignature.erase(): MethodTypeDesc {
  return MethodTypeDesc.of(result().erase(), arguments().map { it.erase() })
}