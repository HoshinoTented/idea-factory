package org.aya.tool.classfile

import kala.collection.Seq
import kala.collection.immutable.ImmutableArray
import kala.collection.immutable.ImmutableSeq
import java.lang.classfile.AccessFlags
import java.lang.classfile.ClassBuilder
import java.lang.classfile.constantpool.InvokeDynamicEntry
import java.lang.classfile.constantpool.MethodHandleEntry
import java.lang.constant.*
import java.lang.invoke.LambdaMetafactory

class ClassBuilderWrapper(
  val classData: ClassData,
  val builder: ClassBuilder,
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
    return FieldData(classData.className, mask, type, name).apply {
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
    vararg parameterTypes: ClassDesc,
    handler: MethodCodeCont,
  ): MethodData {
    return method(returnType, methodName, ImmutableArray.Unsafe.wrap(parameterTypes), handler)
  }
  
  fun AccessFlagBuilder.method(
    returnType: ClassDesc,
    methodName: String,
    parameterType: Seq<ClassDesc>,
    handler: MethodCodeCont,
  ): MethodData {
    val flags = AccessFlags.ofMethod(this@method.mask())
    val data = MethodData(
      classData.className, methodName, flags,
      MethodTypeDesc.of(returnType, parameterType.asJava()),
      false
    )
    
    data.build(this@ClassBuilderWrapper) {
      handler.invoke(this, ArgumentProvider(data))
    }
    
    return data
  }
  
  fun AccessFlagBuilder.constructor(
    superConstructor: MethodData,
    superArguments: Seq<CodeCont>,
    handler: MethodCodeCont0,
  ): MethodData {
    return constructor(superConstructor, superArguments, ImmutableSeq.empty(), methodHandler0(handler))
  }
  
  fun AccessFlagBuilder.constructor(
    superConstructor: MethodData,
    superArguments: Seq<CodeCont>,
    parameterType0: ClassDesc,
    handler: MethodCodeCont1,
  ): MethodData {
    return constructor(superConstructor, superArguments, ImmutableSeq.of(parameterType0), methodHandler1(handler))
  }
  
  fun AccessFlagBuilder.constructor(
    superConstructor: MethodData,
    superArguments: Seq<CodeCont>,
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
    superArguments: Seq<CodeCont>,
    parameterType: Seq<ClassDesc>,
    handler: MethodCodeCont,
  ): MethodData {
    anyConstructor = true
    return method(ConstantDescs.CD_void, ConstantDescs.INIT_NAME, parameterType) {
      invokespecial(CodeBuilderWrapper.thisRef, superConstructor, superArguments)
      handler.invoke(this, it)
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
  fun makeLambda(
    interfaceMethod: MethodData,
    captureTypes: ImmutableSeq<ClassDesc>,
    handler: LambdaCodeCont
  ): InvokeDynamicEntry {
    val pool = builder.constantPool()
    
    // build lambda method
    val lambdaMethodName = this.lambdaMethodName()
    val fullParam = captureTypes.view().appendedAll(interfaceMethod.signature.parameterList())
    
    val lambdaMethodData = private().static().synthetic().method(
      interfaceMethod.signature.returnType(),
      lambdaMethodName,
      fullParam.toImmutableSeq(),
    ) {
      handler.invoke(this@method, LambdaArgumentProvider(captureTypes, interfaceMethod.signature))
    }
    
    // name: the only abstract method that functional interface defines
    // type: returns the functional interface, parameters are captures
    val nameAndType = pool.nameAndTypeEntry(
      interfaceMethod.methodName, MethodTypeDesc.of(
        interfaceMethod.inClass,
        captureTypes.toList()
      )
    )
    
    // 0th: erased function signature
    // 1st: the function name to the lambda
    // 2nd: function signature
    val bsm = pool.bsmEntry(
      lambdaBootstrapMethodHandle, listOf(
        interfaceMethod.signature,
        lambdaMethodData.makeMethodHandle(),
        interfaceMethod.signature,
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
      Seq.empty(),
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