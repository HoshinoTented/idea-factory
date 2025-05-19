package org.aya.tool.classfile.data

import kala.collection.immutable.ImmutableSeq
import kala.collection.immutable.ImmutableSet
import kala.collection.mutable.MutableMap
import org.aya.tool.classfile.AccessFlagBuilder
import org.aya.tool.classfile.AccessFlagSet
import org.aya.tool.classfile.ClassBuilderWrapper
import org.aya.tool.classfile.ClassOutput
import org.aya.tool.classfile.DefaultClassOutput
import org.aya.tool.classfile.asDesc
import java.lang.classfile.ClassBuilder
import java.lang.classfile.ClassFile
import java.lang.classfile.ClassFileBuilder
import java.lang.classfile.constantpool.MethodRefEntry
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.lang.reflect.AccessFlag

interface ClassRef {
  val descriptor: ClassDesc
  val polyCount: Int
}

interface ClassData : ClassRef {
  val flags: AccessFlagSet
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
    val extraFlag = if (!flags.has(AccessFlag.INTERFACE)) AccessFlag.SUPER else null
    val bytecodeOutput = file.build(descriptor) { cb: ClassBuilder ->
      // initialize access flags
      val allFlags = if (extraFlag != null) flags + extraFlag else flags
      cb.withFlags(*allFlags.toArray())
      
      // initialize super class and interfaces
      val superclass = superclass
      val interfaces = interfaces
      cb.withSuperclass(superclass)
      if (interfaces.isNotEmpty) cb.withInterfaceSymbols(interfaces.asJava())
      
      // initialize class body
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
  flags: AccessFlagSet,
  className: ClassDesc,
  interfaces: ImmutableSeq<ClassDesc>,
  superclass: ClassDesc,
): ClassData {
  return ClassDataImpl(flags, className, interfaces, superclass)
}

fun ClassData(className: ClassDesc): ClassData {
  return ClassData(
    AccessFlagSet(AccessFlag.PUBLIC),
    className,
    ImmutableSeq.empty(),
    Object::class.java.asDesc()
  )
}

fun ClassData(clazz: Class<*>): ClassData {
  return ClassDataWrapper(clazz)
}


class ClassDataImpl(
  override val flags: AccessFlagSet,
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
  
  override val flags: AccessFlagSet by lazy {
    AccessFlagSet(ImmutableSet.from(clazz.accessFlags()))
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

/// region inner class

interface InnerClassData : ClassData {
  val outer: ClassData
  override val className: String
}

fun ClassBuilderWrapper.InnerClassData(
  flags: AccessFlagBuilder,
  className: String,
  superclass: ClassDesc = ConstantDescs.CD_Object,
  interfaces: ImmutableSeq<ClassDesc> = ImmutableSeq.empty(),
): InnerClassData {
  return DefaultInnerClassData(flags.build(), this.classData, className, superclass, interfaces)
}

class DefaultInnerClassData(
  override val flags: AccessFlagSet,
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

/// endregion inner class