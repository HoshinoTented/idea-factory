package org.aya.tool.classfile

import kala.collection.immutable.ImmutableSeq
import kala.collection.mutable.MutableMap
import java.lang.classfile.*
import java.lang.classfile.attribute.SignatureAttribute
import java.lang.classfile.constantpool.ConstantPoolBuilder
import java.lang.classfile.constantpool.MethodRefEntry
import java.lang.constant.*
import java.lang.reflect.AccessFlag

/// region ClassData

interface ClassData : ClassRef {
  val flags: AccessFlags
  override val descriptor: ClassDesc
  val interfaces: ImmutableSeq<ClassDesc>
  val superclass: ClassDesc
  override val polyCount: Int
  
  /**
   * The simple name of this class, note that it might not be the class file name because there is nested class.
   */
  val className: String get() = descriptor.displayName()
  val sourceClassName: String get() = className.substringAfterLast('$')
  
  fun build(file: ClassFile, parent: DefaultClassOutput?, handler: ClassBuilderWrapper.() -> Unit): ClassOutput {
    val output: DefaultClassOutput = parent ?: DefaultClassOutput(MutableMap.create())
    val extraMask = if (!flags.has(AccessFlag.INTERFACE)) AccessFlag.SUPER.mask() else 0x0
    val bytecodeOutput = file.build(descriptor) { cb: ClassBuilder ->
      cb.withFlags(flags.flagsMask() or extraMask)
      
      val superclass = superclass
      val interfaces = interfaces
      cb.withSuperclass(superclass)
      
      if (interfaces.isNotEmpty) cb.withInterfaceSymbols(interfaces.asJava())
      
      val cbw = ClassBuilderWrapper(this, cb, output)
      handler.invoke(cbw)
      cbw.done()
    }
    
    output.addOutput(descriptor.displayName(), bytecodeOutput)
    return output
  }
  
  fun build(file: ClassFile, handler: ClassBuilderWrapper.() -> Unit): ClassOutput {
    return build(file, null, handler)
  }
  
  fun makeConstructorRefEntry(
    builder: ClassFileBuilder<*, *>,
    vararg parameterType: ClassDesc,
  ): MethodRefEntry {
    val pool = builder.constantPool()
    return pool.methodRefEntry(
      descriptor,
      ConstantDescs.INIT_NAME,
      MethodTypeDesc.of(ConstantDescs.CD_void, *parameterType)
    )
  }
}

fun ClassData(
  flags: AccessFlags,
  className: ClassDesc,
  interfaces: ImmutableSeq<ClassDesc>,
  superclass: ClassDesc,
): ClassData {
  return ClassDataImpl(flags, className, interfaces, superclass)
}

fun ClassData(className: ClassDesc): ClassData {
  return ClassData(
    AccessFlags.ofClass(AccessFlag.PUBLIC),
    className,
    ImmutableSeq.empty(),
    Object::class.java.asDesc()
  )
}

fun ClassData(clazz: Class<*>): ClassData {
  return ClassDataWrapper(clazz)
}


class ClassDataImpl(
  override val flags: AccessFlags,
  override val descriptor: ClassDesc,
  override val interfaces: ImmutableSeq<ClassDesc>,
  override val superclass: ClassDesc,
) : ClassData {
  override val polyCount: Int = 0
}

class ClassDataWrapper(val clazz: Class<*>) : ClassData {
  override val descriptor: ClassDesc by lazy {
    clazz.asDesc()
  }
  
  override val flags: AccessFlags by lazy {
    val flagsMark = clazz.accessFlags().fold(0x0) { acc, flag ->
      acc or flag.mask()
    }
    
    AccessFlags.ofClass(flagsMark)
  }
  
  override val interfaces: ImmutableSeq<ClassDesc> by lazy {
    ImmutableSeq.from(clazz.interfaces).map { it.asDesc() }
  }
  
  override val superclass: ClassDesc by lazy {
    clazz.superclass.asDesc()
  }
  
  override val polyCount: Int by lazy {
    clazz.typeParameters.size
  }
}

interface InnerClassData : ClassData {
  val outer: ClassData
  override val className: String
}

fun ClassBuilderWrapper.InnerClassData(
  flags: AccessFlagBuilder,
  className: String,
  superclass: ClassDesc = ConstantDescs.CD_Object,
  interfaces: ImmutableSeq<ClassDesc> = ImmutableSeq.empty()
): InnerClassData {
  return DefaultInnerClassData(AccessFlags.ofClass(flags.mask()), this.classData, className, superclass, interfaces)
}

class DefaultInnerClassData(
  override val flags: AccessFlags,
  override val outer: ClassData,
  override val className: String,
  override val superclass: ClassDesc,
  override val interfaces: ImmutableSeq<ClassDesc>,
) : InnerClassData {
  override val descriptor: ClassDesc by lazy {
    outer.descriptor.nested(className)
  }
  
  override val polyCount: Int = 0
}

/// endregion ClassData

/// region FieldData

@JvmRecord
data class FieldData(
  val owner: ClassDesc,
  val flags: AccessFlags,
  val returnType: ClassDesc,
  val name: String,
) {
  fun build(cb: ClassBuilder) {
    cb.withField(name, returnType, flags.flagsMask())
  }
}

/// endregion FieldData

/// region MethodData

interface MethodData : MethodRef {
  override val owner: ClassDesc
  override val name: String
  override val signature: MethodSignature
  val flags: AccessFlags
  
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
      name, descriptor, flags.flagsMask()
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
  flags: AccessFlags,
  signature: MethodSignature,
  isInterface: Boolean,
): MethodData {
  return MethodDataImpl(inClass, methodName, signature, flags, isInterface)
}

fun MethodData(
  inClass: ClassDesc,
  methodName: String,
  flags: AccessFlags,
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
  flags: AccessFlags,
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
  override val flags: AccessFlags,
  override val isInterface: Boolean,
) : MethodData

/// endregion MethodData