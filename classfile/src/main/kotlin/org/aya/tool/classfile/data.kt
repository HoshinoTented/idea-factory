package org.aya.tool.classfile

import kala.collection.immutable.ImmutableSeq
import java.lang.classfile.*
import java.lang.classfile.constantpool.ConstantPoolBuilder
import java.lang.classfile.constantpool.MethodRefEntry
import java.lang.constant.*
import java.lang.reflect.AccessFlag

/// region ClassData

interface ClassData {
  val flags: AccessFlags
  val className: ClassDesc
  val interfaces: ImmutableSeq<ClassData>
  val superclass: ClassData
  
  fun build(file: ClassFile, handler: ClassBuilderWrapper.() -> Unit): ByteArray {
    return file.build(className) { cb: ClassBuilder ->
      cb.withFlags(flags.flagsMask())
      
      val superclass = superclass
      val interfaces = interfaces
      cb.withSuperclass(superclass.className)
      
      if (interfaces.isNotEmpty) cb.withInterfaceSymbols(interfaces.map { it.className }.asJava())
      
      val cbw = ClassBuilderWrapper(this, cb)
      handler.invoke(cbw)
      cbw.done()
    }
  }
  
  fun makeConstructorRefEntry(
    builder: ClassFileBuilder<*, *>,
    vararg parameterType: ClassDesc,
  ): MethodRefEntry {
    val pool = builder.constantPool()
    return pool.methodRefEntry(
      className,
      ConstantDescs.INIT_NAME,
      MethodTypeDesc.of(ConstantDescs.CD_void, *parameterType)
    )
  }
}

fun ClassData(
  flags: AccessFlags,
  className: ClassDesc,
  interfaces: ImmutableSeq<ClassData>,
  superclass: ClassData,
): ClassData {
  return ClassDataImpl(flags, className, interfaces, superclass)
}

fun ClassData(className: ClassDesc): ClassData {
  return ClassData(
    AccessFlags.ofClass(AccessFlag.PUBLIC),
    className,
    ImmutableSeq.empty(),
    ClassData(Object::class.java)
  )
}

fun ClassData(clazz: Class<*>): ClassData {
  return ClassDataWrapper(clazz)
}

@JvmRecord
data class ClassDataImpl(
  override val flags: AccessFlags,
  override val className: ClassDesc,
  override val interfaces: ImmutableSeq<ClassData>,
  override val superclass: ClassData,
) : ClassData

data class ClassDataWrapper(val clazz: Class<*>) : ClassData {
  override val className: ClassDesc by lazy {
    clazz.asDesc()
  }
  
  override val flags: AccessFlags by lazy {
    val flagsMark = clazz.accessFlags().fold(0x0) { acc, flag ->
      acc or flag.mask()
    }
    
    AccessFlags.ofClass(flagsMark)
  }
  
  override val interfaces: ImmutableSeq<ClassData> by lazy {
    ImmutableSeq.from(clazz.interfaces).map { ClassDataWrapper(it) }
  }
  
  override val superclass: ClassData by lazy {
    ClassDataWrapper(clazz.superclass)
  }
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

interface MethodData {
  val inClass: ClassData
  val methodName: String
  val flags: AccessFlags
  val signature: MethodTypeDesc
  val isInterface: Boolean
  val isStatic get() = flags.has(AccessFlag.STATIC)
  
  fun kind(): DirectMethodHandleDesc.Kind {
    if (flags.has(AccessFlag.STATIC))
      return if (isInterface) DirectMethodHandleDesc.Kind.INTERFACE_STATIC
      else DirectMethodHandleDesc.Kind.STATIC
    if (methodName == ConstantDescs.INIT_NAME || methodName == ConstantDescs.CLASS_INIT_NAME)
      return if (isInterface) DirectMethodHandleDesc.Kind.INTERFACE_SPECIAL     // TODO: I am not sure
      else DirectMethodHandleDesc.Kind.SPECIAL
    return if (isInterface) DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL
    else DirectMethodHandleDesc.Kind.VIRTUAL
  }
  
  fun makeMethodHandle(): DirectMethodHandleDesc {
    return MethodHandleDesc.ofMethod(
      kind(),
      inClass.className,
      methodName,
      signature
    )
  }
  
  fun build(cb: ClassBuilderWrapper, build: CodeCont) {
    val isStatic = isStatic
    val usedSlot = (if (isStatic) 0 else 1) + signature.parameterCount()
    cb.builder.withMethodBody(
      methodName, signature, flags.flagsMask()
    ) { codeBuilder ->
      build.invoke(CodeBuilderWrapper(cb, codeBuilder, VariablePool(usedSlot - 1), !isStatic))
    }
  }
  
  fun makeMethodRefEntry(builder: ConstantPoolBuilder): MethodRefEntry {
    return builder.methodRefEntry(inClass.className, methodName, signature)
  }
}

fun MethodData(
  inClass: ClassData,
  methodName: String,
  flags: AccessFlags,
  signature: MethodTypeDesc,
  isInterface: Boolean,
): MethodData {
  return MethodDataImpl(inClass, methodName, flags, signature, isInterface)
}

@JvmRecord
data class MethodDataImpl(
  override val inClass: ClassData,
  override val methodName: String,
  override val flags: AccessFlags,
  override val signature: MethodTypeDesc,
  override val isInterface: Boolean,
) : MethodData {
}

/// endregion MethodData