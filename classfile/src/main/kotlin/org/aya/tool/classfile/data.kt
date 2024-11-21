package org.aya.tool.classfile

import kala.collection.Seq
import java.lang.classfile.*
import java.lang.classfile.constantpool.ConstantPoolBuilder
import java.lang.classfile.constantpool.MethodRefEntry
import java.lang.constant.*
import java.lang.reflect.AccessFlag

/// region ClassData

@JvmRecord
data class ClassData(
  val flags: AccessFlags,
  val className: ClassDesc,
  val interfaces: Seq<ClassDesc>,
  val superclass: ClassDesc,
) {
  constructor(className: ClassDesc) : this(
    AccessFlags.ofClass(AccessFlag.PUBLIC),
    className,
    Seq.empty<ClassDesc>(),
    ConstantDescs.CD_Object
  )
  
  fun build(file: ClassFile, handler: ClassBuilderWrapper.() -> Unit): ByteArray {
    return file.build(className) { cb: ClassBuilder ->
      cb.withFlags(flags.flagsMask())
      
      val superclass = superclass
      val interfaces = interfaces
      cb.withSuperclass(superclass)
      
      if (interfaces.isNotEmpty) cb.withInterfaceSymbols(interfaces.asJava())
      
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

/// endregion ClassData

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

@JvmRecord
data class MethodData(
  val inClass: ClassDesc,
  val methodName: String,
  val flags: AccessFlags,
  val signature: MethodTypeDesc,
  val isInterface: Boolean,
) {
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
      inClass,
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
      build.invoke(CodeBuilderWrapper(cb, codeBuilder, VariablePool(usedSlot), !isStatic))
    }
  }
  
  fun makeMethodRefEntry(builder: ConstantPoolBuilder): MethodRefEntry {
    return builder.methodRefEntry(inClass, methodName, signature)
  }
}