package org.aya.tool.classfile

import java.lang.classfile.MethodSignature
import java.lang.classfile.Signature
import java.lang.constant.ClassDesc
import kala.collection.mutable.MutableList
import kala.tuple.primitive.IntObjTuple2
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc

class SignatureBuilder(
  private val buffer: MutableList<Signature>,
  private var polyCount: Int,
  private val typeVars: MutableList<Signature.TypeParam>,
) {
  fun typeVar(classBound: ClassDesc? = null, vararg interfaceBounds: ClassDesc): Signature.TypeVarSig {
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
    is Signature.BaseTypeSig -> ClassDesc.of(this.signatureString())
    is Signature.ArrayTypeSig -> {
      val decomposed = this.decompose()
      val depth = decomposed.component1
      val elementType = decomposed.component2
      elementType.erase().arrayType(depth)
    }
    is Signature.ClassTypeSig -> ClassDesc.ofInternalName(className())
    is Signature.TypeVarSig -> ConstantDescs.CD_Object
  }
}

fun Signature.ArrayTypeSig.decompose(): IntObjTuple2<Signature> {
  var depth = 0
  var sig: Signature = this
  
  while (sig is Signature.ArrayTypeSig) {
    depth += 1
    sig = sig.componentSignature()
  }
  
  return IntObjTuple2.of(depth, sig)
}

fun MethodSignature.erase(): MethodTypeDesc {
  return MethodTypeDesc.of(result().erase(), arguments().map { it.erase() })
}