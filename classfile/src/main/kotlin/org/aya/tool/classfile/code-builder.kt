package org.aya.tool.classfile

import kala.collection.Seq
import kala.collection.immutable.ImmutableArray
import kala.collection.immutable.ImmutableSeq
import org.jetbrains.annotations.Contract
import java.lang.classfile.CodeBuilder
import java.lang.classfile.Opcode
import java.lang.classfile.TypeKind
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.DirectMethodHandleDesc
import java.lang.reflect.AccessFlag

class CodeBuilderWrapper constructor(
  private val inClassFile: ClassBuilderWrapper,
  val builder: CodeBuilder,
  val pool: VariablePool,
  private val hasThis: Boolean
) {
  /// region invoke
  internal enum class InvokeKind {
    Interface,
    Virtual,
    Static,
    Special
  }
  
  internal fun invoke(
    invokeKind: InvokeKind,
    theObject: CodeCont?,
    theMethod: MethodData,
    args: Seq<CodeCont>,
  ): CodeBuilderWrapper {
    theObject?.invoke(this)
    args.view().reversed().forEach { f ->
      f.invoke(this)
    }
    
    val owner = theMethod.inClass.className
    val name = theMethod.methodName
    val type = theMethod.signature
    
    when (invokeKind) {
      InvokeKind.Interface -> builder.invokeinterface(owner, name, type)
      InvokeKind.Virtual -> builder.invokevirtual(owner, name, type)
      InvokeKind.Static -> builder.invokestatic(owner, name, type)
      InvokeKind.Special -> builder.invokespecial(owner, name, type)
    }
    
    return this
  }
  
  /// endregion invoke
  
  /// region method helper
  
  data class MethodRef(val data: MethodData, val obj: CodeCont)
  
  @Contract(pure = true)
  fun MethodData.of(obj: CodeBuilderWrapper.() -> Unit): MethodRef {
    assert(!isStatic) { "static" }
    return MethodRef(this@of, obj)
  }
  
  fun MethodData.invoke(vararg args: CodeCont): ExprCont {
    assert(isStatic) { "not static" }
    val argSeq = ImmutableArray.Unsafe.wrap<CodeCont>(args)
    return ExprCont(this.signature.returnType()) {
      invoke(InvokeKind.Static, null, this@invoke, argSeq)
    }
  }
  
  @Contract(pure = true)
  fun MethodRef.invoke(vararg args: CodeCont): ExprCont {
    val argSeq = ImmutableArray.Unsafe.wrap<CodeCont>(args)
    val cont: CodeCont = when (data.kind()) {
      DirectMethodHandleDesc.Kind.VIRTUAL -> {
        { invoke(InvokeKind.Virtual, obj, data, argSeq) }
      }
      
      DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL -> {
        { invoke(InvokeKind.Interface, obj, data, argSeq) }
      }
      
      DirectMethodHandleDesc.Kind.SPECIAL,
      DirectMethodHandleDesc.Kind.INTERFACE_SPECIAL -> {
        { invoke(InvokeKind.Special, obj, data, argSeq) }
      }
      
      else -> throw IllegalArgumentException("not a suitable method")
    }
    
    return ExprCont(this.data.signature.returnType(), cont)
  }
  
  /**
   * Invoke a [CodeCont] immediately,
   * this is used on an [ExprCont] usually, in order to discard the result.
   */
  operator fun CodeCont.unaryPlus() {
    this@unaryPlus.invoke(this@CodeBuilderWrapper)
  }
  
  @Contract(pure = true)
  fun MethodData.nu(vararg args: CodeCont): ExprCont = ExprCont(this.inClass.className) {
    builder.new_(this@nu.inClass.className)
    // invoke constructor
    invoke(InvokeKind.Special, { builder.dup() }, this@nu, ImmutableArray.Unsafe.wrap(args))
  }
  
  fun ret(value: ExprCont? = null) {
    if (value == null) {
      builder.return_()
    } else {
      val ty = assertValidType(value.type)
      +value
      builder.returnInstruction(TypeKind.fromDescriptor(ty.descriptorString()))
    }
  }
  
  /// endregion method helper
  
  /// region field helper
  
  /**
   * A "reference" to a field of certain object, the instruction is wrote until the command is given.
   */
  data class FieldRef(val data: FieldData, val obj: CodeCont)
  
  @Contract(pure = true)
  fun FieldData.of(obj: CodeCont): FieldRef {
    assert(!this.flags.has(AccessFlag.STATIC))
    return FieldRef(this@of, obj)
  }
  
  fun FieldData.set(value: CodeCont) {
    value.invoke(this@CodeBuilderWrapper)
    builder.putstatic(this.owner, this.name, this.returnType)
  }
  
  @Contract(pure = true)
  fun FieldData.get(): ExprCont = ExprCont(returnType) {
    builder.getstatic(owner, name, returnType)
  }
  
  fun FieldRef.set(value: CodeCont) {
    obj.invoke(this@CodeBuilderWrapper)
    value.invoke(this@CodeBuilderWrapper)
    with(this.data) {
      builder.putfield(owner, name, returnType)
    }
  }
  
  @Contract(pure = true)
  fun FieldRef.get(): ExprCont = ExprCont(data.returnType) {
    obj.invoke(this@CodeBuilderWrapper)
    builder.getfield(data.owner, data.name, data.returnType)
  }
  
  /// endregion field helper
  
  /// region expression helper
  
  interface ExprCont : (CodeBuilderWrapper) -> Unit {
    companion object {
      fun ofVoid(cont: CodeCont): ExprCont {
        return ExprCont(ConstantDescs.CD_void, cont)
      }
    }
    
    val type: ClassDesc
    
    operator fun not(): ExprCont {
      assert(isBoolean(type))
      return ExprCont(ConstantDescs.CD_boolean) {
        invoke(this)
        builder.ifThenElse(Opcode.IFNE, {
          // if cont == true
          builder.iconst_0()
        }, {
          // if cont == false
          builder.iconst_1()
        })
      }
    }
    
    fun instanceof(type: ClassDesc): ExprCont {
      assert(isObject(type))
      return ExprCont(ConstantDescs.CD_boolean) {
        invoke(this)
        builder.instanceof_(type)
      }
    }
  }
  
  
  /**
   * A [CodeCont] that push an expression to the stack, with type information [type].
   * Note that [type] may not be a valid type, e.g. `void`.
   */
  data class ExprContImpl(override val type: ClassDesc, val cont: CodeCont) : ExprCont {
    override fun invoke(p1: CodeBuilderWrapper) {
      cont.invoke(p1)
    }
  }
  
  @get:Contract(pure = true)
  val self: ExprCont by lazy {
    if (hasThis) ExprCont(inClassFile.classData.className, thisRef) else {
      throw IllegalStateException("static")
    }
  }
  
  @Contract(pure = true)
  fun nil(ty: ClassDesc) = ExprCont(ty, nilRef)
  
  fun subscoped(builder: CodeBuilder, newPool: VariablePool): CodeBuilderWrapper {
    return CodeBuilderWrapper(inClassFile, builder, newPool, hasThis)
  }
  
  inline fun <R> subscoped(builder: CodeBuilder, block: CodeBuilderWrapper.() -> R): R {
    return this.pool.subscoped {
      subscoped(builder, it).block()
    }
  }
  
  /// endregion argument helper
  
  /// region local variable helper
  
  data class Variable(override val type: ClassDesc, val slot: Int) : ExprCont {
    init {
      assertValidType(type)
    }
    
    override fun invoke(p1: CodeBuilderWrapper) {
      p1.builder.loadInstruction(type.typeKind, slot)
    }
  }
  
  fun let(type: ClassDesc): Variable = Variable(type, pool.acquire())
  
  @Contract(pure = true)
  fun Variable.get(): ExprCont = this
  
  @Contract(pure = true)
  fun Variable.getArr(idx: Int): ExprCont {
    val ty = type.componentType()
      ?: throw AssertionError("not an array")
    
    return ExprCont(ty) {
      builder.iconst(idx)
      builder.arrayLoadInstruction(ty.typeKind)
    }
  }
  
  fun Variable.set(bool: Boolean) {
    isBoolean(type)
    
    if (bool) {
      builder.iconst_1()
    } else {
      builder.iconst_0()
    }
    
    builder.istore(slot)
  }
  
  fun Variable.set(i: Int) {
    isInteger(type)
    
    builder.iconst(i)
    builder.istore(slot)
  }
  
  fun Variable.set(value: ExprCont) {
    // variable type is never `void`, so we don't have to check the result
    assertTypeMatch(type, value.type)
    value.invoke(this@CodeBuilderWrapper)
    builder.storeInstruction(type.typeKind, slot)
  }
  
  fun Variable.setArr(idx: Int, value: ExprCont) {
    val ty = type.componentType()
      ?: throw AssertionError("not an array")
    
    builder.iconst(idx)
    value.invoke(this@CodeBuilderWrapper)
    builder.arrayStoreInstruction(ty.typeKind)
  }
  
  /// endregion local variable helper
  
  /// region lambda helper
  
  fun MethodData.lambda(vararg captures: ExprCont, handler: LambdaCodeCont): ExprCont {
    val entry = inClassFile.makeLambda(
      this,
      ImmutableSeq.from(captures.map { it.type }),
      handler
    )
    
    return ExprCont(this.inClass.className) {
      ImmutableSeq.from(captures).view().reversed().forEach {
        it.invoke(this@CodeBuilderWrapper)
      }
      
      builder.invokedynamic(entry)
    }
  }
  
  /// endregion
  
  /// region if helper
  
  data class CondExprCont(val opcode: Opcode, val args: ImmutableSeq<ExprCont>)
  data class IfThenElseBlock(val ifThenBlock: IfThenBlock, val elseBlock: ExprCont)
  
  data class IfThenBlock(val cond: ExprCont, val thenBlock: ExprCont) {
    @Contract(pure = true)
    fun orElse(elseBlock: CodeCont): IfThenElseBlock {
      return orElse(ExprCont.ofVoid(elseBlock))
    }
    
    @Contract(pure = true)
    fun orElse(elseBlock: ExprCont): IfThenElseBlock {
      return IfThenElseBlock(this, elseBlock)
    }
  }
  
  fun IfThenBlock.end() {
    cond.invoke(this@CodeBuilderWrapper)
    builder.ifThen {
      subscoped(it) {
        thenBlock.invoke(this@subscoped)
      }
    }
  }
  
  @Contract(pure = true)
  fun IfThenElseBlock.expr(): ExprCont {
    val ifType = ifThenBlock.thenBlock.type
    val elseType = elseBlock.type
    
    if (!assertTypeMatch(ifType, elseType)) {
      throw AssertionError("blocks return void, use `.end()` instead.")
    }
    
    return ExprCont(elseBlock.type) {
      this@expr.end()
    }
  }
  
  fun IfThenElseBlock.end() {
    ifThenBlock.cond.invoke(this@CodeBuilderWrapper)
    val thenBlock = ifThenBlock.thenBlock
    builder.ifThenElse({
      subscoped(it) {
        thenBlock.invoke(this@subscoped)
      }
    }, {
      subscoped(it) {
        elseBlock.invoke(this@subscoped)
      }
    })
  }
  
  @Contract(pure = true)
  fun ifThen(cond: ExprCont, thenBlock: ExprCont): IfThenBlock {
    assert(isBoolean(cond.type))
    return IfThenBlock(cond, thenBlock)
  }
  
  @Contract(pure = true)
  fun ifThen(cond: ExprCont, thenBlock: CodeCont): IfThenBlock {
    return ifThen(cond, ExprCont.ofVoid(thenBlock))
  }
  
  /// endregion if helper
  
  companion object {
    val thisRef: CodeCont = { builder.aload(0) }
    val nilRef: CodeCont = { builder.aconst_null() }
    val ya: ExprCont = ExprCont(ConstantDescs.CD_boolean) { builder.iconst(1) }
    val nein: ExprCont = ExprCont(ConstantDescs.CD_boolean) { builder.iconst(0) }
    
    fun ExprCont(type: ClassDesc, cont: CodeCont): ExprCont {
      return ExprContImpl(type, cont)
    }
  }
}
