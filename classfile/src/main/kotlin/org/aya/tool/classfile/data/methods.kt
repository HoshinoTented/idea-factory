package org.aya.tool.classfile.data

import kala.collection.immutable.ImmutableSeq
import org.aya.tool.classfile.AccessFlagSet
import org.aya.tool.classfile.ClassBuilderWrapper
import org.aya.tool.classfile.CodeBuilderWrapper
import org.aya.tool.classfile.CodeCont
import org.aya.tool.classfile.DefaultVariablePool
import org.aya.tool.classfile.ParameterizedSignature
import org.aya.tool.classfile.buildSignature
import org.aya.tool.classfile.erase
import java.lang.classfile.MethodSignature
import java.lang.classfile.attribute.SignatureAttribute
import java.lang.classfile.constantpool.ConstantPoolBuilder
import java.lang.classfile.constantpool.MethodRefEntry
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.DirectMethodHandleDesc
import java.lang.constant.MethodHandleDesc
import java.lang.constant.MethodTypeDesc
import java.lang.reflect.AccessFlag

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

/**
 * All information of a method
 */
interface MethodData : MethodRef {
  override val owner: ClassDesc
  override val name: String
  override val signature: MethodSignature
  val flags: AccessFlagSet
  
  val isInterface: Boolean
  val isStatic get() = flags.has(AccessFlag.STATIC)
  
  override val invokeKind: DirectMethodHandleDesc.Kind
    get() {
      if (flags.has(AccessFlag.STATIC))
        return if (isInterface) DirectMethodHandleDesc.Kind.INTERFACE_STATIC
        else DirectMethodHandleDesc.Kind.STATIC
      if (name == ConstantDescs.INIT_NAME || name == ConstantDescs.CLASS_INIT_NAME)
        return if (isInterface) DirectMethodHandleDesc.Kind.INTERFACE_SPECIAL     // TODO: I am not sure
        else DirectMethodHandleDesc.Kind.SPECIAL
      return if (isInterface) DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL
      else DirectMethodHandleDesc.Kind.VIRTUAL
    }
  
  fun makeMethodHandle(): DirectMethodHandleDesc {
    return MethodHandleDesc.ofMethod(
      invokeKind,
      owner,
      name,
      descriptor
    )
  }
  
  fun build(cb: ClassBuilderWrapper, build: CodeCont) {
    val isStatic = isStatic
    val usedSlot = (if (isStatic) 0 else 1) + signature.arguments().size
    cb.builder.withMethod(
      name, descriptor, flags.mask(AccessFlag.Location.METHOD)
    ) { mb ->
      mb.with(SignatureAttribute.of(signature))
      mb.withCode {
        build.invoke(CodeBuilderWrapper(cb, it, DefaultVariablePool(usedSlot - 1), !isStatic))
      }
    }
  }
  
  fun makeMethodRefEntry(builder: ConstantPoolBuilder): MethodRefEntry {
    return builder.methodRefEntry(owner, name, descriptor)
  }
}

fun MethodData(
  inClass: ClassDesc,
  methodName: String,
  flags: AccessFlagSet,
  signature: MethodSignature,
  isInterface: Boolean,
): MethodData {
  return MethodDataImpl(inClass, methodName, signature, flags, isInterface)
}

fun MethodData(
  inClass: ClassDesc,
  methodName: String,
  flags: AccessFlagSet,
  signature: MethodTypeDesc,
  isInterface: Boolean,
): MethodData {
  return MethodData(
    inClass,
    methodName,
    flags,
    ImmutableSeq.from(signature.parameterList()),
    signature.returnType(),
    isInterface
  )
}

fun MethodData(
  inClass: ClassDesc,
  methodName: String,
  flags: AccessFlagSet,
  parameters: ImmutableSeq<ClassDesc>,
  returnType: ClassDesc,
  isInterface: Boolean,
): MethodData {
  return MethodDataImpl(inClass, methodName, buildSignature {
    parameters.forEach { +it }
  }.ret(returnType), flags, isInterface)
}

class MethodDataImpl(
  override val owner: ClassDesc,
  override val name: String,
  override val signature: MethodSignature,
  override val flags: AccessFlagSet,
  override val isInterface: Boolean,
) : MethodData