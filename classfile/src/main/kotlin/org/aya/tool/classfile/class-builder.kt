package org.aya.tool.classfile

import kala.collection.Seq
import kala.collection.immutable.ImmutableSeq
import java.lang.classfile.AccessFlags
import java.lang.classfile.ClassBuilder
import java.lang.classfile.ClassFile
import java.lang.classfile.attribute.InnerClassInfo
import java.lang.classfile.attribute.InnerClassesAttribute
import java.lang.classfile.constantpool.InvokeDynamicEntry
import java.lang.classfile.constantpool.MethodHandleEntry
import java.lang.constant.*
import java.lang.invoke.LambdaMetafactory
import java.lang.reflect.AccessFlag
import java.util.*

class ClassBuilderWrapper(
  val classData: ClassData,
  val builder: ClassBuilder,
  internal val classOutput: DefaultClassOutput
) {
  private var anyConstructor: Boolean = false
  private val lambdaBootstrapMethodHandle: MethodHandleEntry by lazy {
    builder.constantPool().methodHandleEntry(
      MethodHandleDesc.ofMethod(
        DirectMethodHandleDesc.Kind.STATIC,
        LambdaMetafactory::class.java.asDesc(),
        "metafactory",
        MTD_LambdaMetafactory_metafactory
      )
    )
  }
  
  private var lambdaCounter: Int = 0
  
  fun AccessFlagBuilder.field(type: ClassDesc, name: String): FieldData {
    val mask = AccessFlags.ofField(this@field.mask())
    return FieldData(classData.descriptor, mask, type, name).apply {
      build(builder)
    }
  }
  
  fun methodHandler0(handler: MethodCodeCont0): MethodCodeCont = {
    handler.invoke(this)
  }
  
  fun methodHandler1(handler: MethodCodeCont1): MethodCodeCont = {
    val arg0 = it.arg(0)
    handler.invoke(this, arg0)
  }
  
  fun methodHandler2(handler: MethodCodeCont2): MethodCodeCont = {
    val arg0 = it.arg(0)
    val arg1 = it.arg(1)
    handler.invoke(this, arg0, arg1)
  }
  
  fun AccessFlagBuilder.method(
    returnType: ClassDesc,
    methodName: String,
    handler: MethodCodeCont0,
  ): MethodData {
    return method(returnType, methodName, ImmutableSeq.empty(), methodHandler0(handler))
  }
  
  fun AccessFlagBuilder.method(
    returnType: ClassDesc,
    methodName: String,
    parameterType0: ClassDesc,
    handler: MethodCodeCont1,
  ): MethodData {
    return method(returnType, methodName, ImmutableSeq.of(parameterType0), methodHandler1(handler))
  }
  
  fun AccessFlagBuilder.method(
    returnType: ClassDesc,
    methodName: String,
    parameterType0: ClassDesc,
    parameterType1: ClassDesc,
    handler: MethodCodeCont2,
  ): MethodData {
    return method(returnType, methodName, ImmutableSeq.of(parameterType0, parameterType1), methodHandler2(handler))
  }
  
  fun AccessFlagBuilder.method(
    returnType: ClassDesc,
    methodName: String,
    parameterType: ImmutableSeq<ClassDesc>,
    handler: MethodCodeCont,
  ): MethodData {
    val flags = AccessFlags.ofMethod(this@method.mask())
    val data = MethodData(
      classData.descriptor, methodName, flags,
      parameterType.map { x -> Parameter.Exact(x) },
      Parameter.Exact(returnType),
      false
    )
    
    data.build(this@ClassBuilderWrapper) {
      handler.invoke(this, DefaultArgumentProvider(data.descriptor, !data.isStatic))
    }
    
    return data
  }
  
  fun AccessFlagBuilder.constructor(
    superConstructor: MethodData,
    superArguments: ImmutableSeq<CodeCont>,
    handler: MethodCodeCont0,
  ): MethodData {
    return constructor(superConstructor, superArguments, ImmutableSeq.empty(), methodHandler0(handler))
  }
  
  fun AccessFlagBuilder.constructor(
    superConstructor: MethodData,
    superArguments: ImmutableSeq<CodeCont>,
    parameterType0: ClassDesc,
    handler: MethodCodeCont1,
  ): MethodData {
    return constructor(superConstructor, superArguments, ImmutableSeq.of(parameterType0), methodHandler1(handler))
  }
  
  fun AccessFlagBuilder.constructor(
    superConstructor: MethodData,
    superArguments: ImmutableSeq<CodeCont>,
    parameterType0: ClassDesc, parameterType1: ClassDesc,
    handler: MethodCodeCont2,
  ): MethodData {
    return constructor(
      superConstructor,
      superArguments,
      ImmutableSeq.of(parameterType0, parameterType1),
      methodHandler2(handler)
    )
  }
  
  fun AccessFlagBuilder.constructor(
    superConstructor: MethodData,
    superArguments: ImmutableSeq<CodeCont>,
    parameterType: ImmutableSeq<ClassDesc>,
    handler: MethodCodeCont,
  ): MethodData {
    anyConstructor = true
    return method(ConstantDescs.CD_void, ConstantDescs.INIT_NAME, parameterType) {
      invoke(CodeBuilderWrapper.InvokeKind.Special, CodeBuilderWrapper.thisRef, superConstructor, superArguments)
      handler.invoke(this, it)
    }
  }
  
  /**
   * We support static inner class for now
   */
  fun InnerClassData.nestedClass(
    file: ClassFile,
    handler: ClassBuilderWrapper.() -> Unit
  ) {
    val attribute = InnerClassesAttribute.of(
      InnerClassInfo.of(
        this.descriptor, Optional.of(this@ClassBuilderWrapper.classData.descriptor),
        Optional.of(this.className), this.flags.flagsMask() or AccessFlag.STATIC.mask()
      )
    )
    
    this@ClassBuilderWrapper.builder.with(attribute)
    this.build(file, this@ClassBuilderWrapper.classOutput) {
      builder.with(attribute)
      handler.invoke(this)
    }
  }
  
  private fun lambdaMethodName(): String {
    val counter = this.lambdaCounter++
    return "lambda$$counter"
  }
  
  /**
   * Note that we don't supply the dynamic type signature, cause this project is used for aya-prover while we
   * need only `() -> Term`.
   */
  internal fun makeLambda(
    interfaceMethod: MethodRef,
    captureTypes: ImmutableSeq<ClassDesc>,
    handler: LambdaCodeCont
  ): InvokeDynamicEntry {
    val pool = builder.constantPool()
    
    // build lambda method
    val lambdaMethodName = this.lambdaMethodName()
    val fullParam = captureTypes.view().appendedAll(interfaceMethod.descriptor.parameterList())
    
    val lambdaMethodData = private().static().synthetic().method(
      interfaceMethod.returnType.erase(),
      lambdaMethodName,
      fullParam.toImmutableSeq(),
    ) {
      handler.invoke(this@method, LambdaArgumentProvider(captureTypes, interfaceMethod.descriptor))
    }
    
    // name: the only abstract method that the functional interface defines
    // type: returns the functional interface, parameters are captures
    val nameAndType = pool.nameAndTypeEntry(
      interfaceMethod.name, MethodTypeDesc.of(
        interfaceMethod.owner,
        captureTypes.asJava()
      )
    )
    
    // 0th: function signature with type parameters erased
    // 1st: the function name to the lambda
    // 2nd: function signature with type parameters substituted
    val bsm = pool.bsmEntry(
      lambdaBootstrapMethodHandle, listOf(
        if (interfaceMethod is ParameterizedSignature) interfaceMethod.base.descriptor else interfaceMethod.descriptor,
        lambdaMethodData.makeMethodHandle(),
        interfaceMethod.descriptor,
      ).map(pool::loadableConstantEntry)
    )
    
    return pool.invokeDynamicEntry(bsm, nameAndType)
  }
  
  /**
   * Make an constructor with no parameter, make sure the superclass has a constructor with no parameter.
   */
  fun defaultConstructor(): MethodData {
    return public().constructor(
      defaultConstructorData(classData.superclass),
      ImmutableSeq.empty(),
    ) { }
  }
  
  fun done() {
    check(anyConstructor) { "Please make at least one constructor" }
  }
}

fun ClassBuilderWrapper.main(handler: MethodCodeCont1): MethodData {
  return public().static().final()
    .method(ConstantDescs.CD_void, MAIN_NAME, Array<String>::class.java.asDesc(), handler)
}