package org.aya.tool.classfile

import kala.collection.Seq
import java.lang.classfile.AccessFlags
import java.lang.classfile.ClassBuilder
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc

class ClassBuilderWrapper(
  val classData: ClassData,
  val builder: ClassBuilder,
) {
  private var anyConstructor: Boolean = false
  
  fun AccessFlagBuilder.field(type: ClassDesc, name: String): FieldData {
    val mask = AccessFlags.ofField(this@field.mask())
    return FieldData(classData.className, mask, type, name).apply {
      build(builder)
    }
  }
  
  fun AccessFlagBuilder.method(
    returnType: ClassDesc,
    methodName: String,
    parameterType: Seq<ClassDesc>,
    handler: CodeCont,
  ): MethodData {
    val flags = AccessFlags.ofMethod(this@method.mask())
    return MethodData(
      classData.className, methodName, flags,
      MethodTypeDesc.of(returnType, parameterType.asJava()),
      false
    ).apply { build(builder, handler) }
  }
  
  fun AccessFlagBuilder.constructor(
    parameterType: Seq<ClassDesc>,
    superConstructor: MethodData,
    superArguments: Seq<CodeCont>,
    handler: CodeCont,
  ): MethodData {
    anyConstructor = true
    return method(ConstantDescs.CD_void, ConstantDescs.INIT_NAME, parameterType) {
      invokespecial(CodeBuilderWrapper.thisRef, superConstructor, superArguments)
      handler.invoke(this)
    }
  }
  
  /**
   * Make an constructor with no parameter, make sure the superclass has a constructor with no parameter.
   */
  fun defaultConstructor(): MethodData {
    return public().constructor(
      Seq.empty(),
      defaultConstructorData(classData.superclass),
      Seq.empty()
    ) { }
  }
  
  fun done() {
    check(anyConstructor) { "Please make at least one constructor" }
  }
}