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

@Suppress("unused")
class CodeBuilderWrapper(
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
    theMethod: MethodRef,
    args: Seq<CodeCont>,
  ): CodeBuilderWrapper {
    theObject?.invoke(this)
    args.view().reversed().forEach { f ->
      f.invoke(this)
    }
    
    val owner = theMethod.owner
    val name = theMethod.name
    val type = theMethod.descriptor
    
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
  
  data class MemberMethodRef(val ref: MethodRef, val obj: CodeCont)
  
  @Contract(pure = true)
  fun MethodRef.of(obj: CodeBuilderWrapper.() -> Unit): MemberMethodRef {
    assert(invokeKind != DirectMethodHandleDesc.Kind.STATIC) { "not static" }
    return MemberMethodRef(this@of, obj)
  }
  
  @Contract(pure = true)
  fun MethodRef.invoke(vararg args: CodeCont): ExprCont {
    assert(invokeKind == DirectMethodHandleDesc.Kind.STATIC) { "static" }
    val argSeq = ImmutableArray.Unsafe.wrap<CodeCont>(args)
    return ExprCont(this.returnType.erase()) {
      invoke(InvokeKind.Static, null, this@invoke, argSeq)
    }
  }
  
  @Contract(pure = true)
  fun MemberMethodRef.invoke(vararg args: CodeCont): ExprCont {
    val argSeq = ImmutableArray.Unsafe.wrap<CodeCont>(args)
    val cont: CodeCont = when (ref.invokeKind) {
      DirectMethodHandleDesc.Kind.VIRTUAL -> {
        { invoke(InvokeKind.Virtual, obj, ref, argSeq) }
      }
      
      DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL -> {
        { invoke(InvokeKind.Interface, obj, ref, argSeq) }
      }
      
      DirectMethodHandleDesc.Kind.SPECIAL,
      DirectMethodHandleDesc.Kind.INTERFACE_SPECIAL -> {
        { invoke(InvokeKind.Special, obj, ref, argSeq) }
      }
      
      else -> throw IllegalArgumentException("not a suitable method")
    }
    
    return ExprCont(this.ref.returnType.erase(), cont)
  }
  
  /**
   * Invoke a [CodeCont] immediately,
   * this is used on an [ExprCont] usually, in order to discard the result.
   */
  operator fun CodeCont.unaryPlus() {
    this@unaryPlus.invoke(this@CodeBuilderWrapper)
  }
  
  @Contract(pure = true)
  fun MethodData.nu(vararg args: CodeCont): ExprCont = ExprCont(this.owner) {
    builder.new_(this@nu.owner)
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
  data class MemberFieldRef(val data: FieldData, val obj: CodeCont)
  
  @Contract(pure = true)
  fun FieldData.of(obj: CodeCont): MemberFieldRef {
    assert(!this.flags.has(AccessFlag.STATIC))
    return MemberFieldRef(this@of, obj)
  }
  
  fun FieldData.set(value: CodeCont) {
    value.invoke(this@CodeBuilderWrapper)
    builder.putstatic(this.owner, this.name, this.returnType)
  }
  
  @Contract(pure = true)
  fun FieldData.get(): ExprCont = ExprCont(returnType) {
    builder.getstatic(owner, name, returnType)
  }
  
  fun MemberFieldRef.set(value: CodeCont) {
    obj.invoke(this@CodeBuilderWrapper)
    value.invoke(this@CodeBuilderWrapper)
    with(this.data) {
      builder.putfield(owner, name, returnType)
    }
  }
  
  @Contract(pure = true)
  fun MemberFieldRef.get(): ExprCont = ExprCont(data.returnType) {
    obj.invoke(this@CodeBuilderWrapper)
    builder.getfield(data.owner, data.name, data.returnType)
  }
  
  /// endregion field helper
  
  /// region expression helper
  
  /**
   * A [CodeCont] that push an expression to the stack, with type information [type].
   * Note that [type] may not be a valid type, e.g. `void`.
   */
  interface ExprCont : (CodeBuilderWrapper) -> Unit {
    companion object {
      fun ofVoid(cont: CodeCont): ExprCont {
        return ExprCont(ConstantDescs.CD_void, cont)
      }
      
      fun from(bool: Boolean): ExprCont = if (bool) ja else nein
    }
    
    val type: ClassDesc
    
    operator fun not(): CondExprCont = CondExprCont(CondExprCont.CondOp.isEq0, ImmutableSeq.of(this))
    
    fun isNull(): CondExprCont = CondExprCont(CondExprCont.CondOp.isNull, ImmutableSeq.of(this))
    
    fun isNotNull(): CondExprCont = CondExprCont(CondExprCont.CondOp.isNotNull, ImmutableSeq.of(this))
    
    fun instanceof(type: ClassDesc): ExprCont {
      assert(type.isClassOrInterface)
      return ExprCont(ConstantDescs.CD_boolean) {
        invoke(this)
        builder.instanceof_(type)
      }
    }
    
    fun cast(type: ClassDesc): ExprCont = ExprCont(type) {
      invoke(this)
      builder.checkcast(type)
    }
  }
  
  data class DefaultExprCont(override val type: ClassDesc, val cont: CodeCont) : ExprCont {
    override fun invoke(p1: CodeBuilderWrapper) {
      cont.invoke(p1)
    }
  }
  
  data class CondExprCont(val opcode: CondOp, val args: ImmutableSeq<ExprCont>) : ExprCont {
    enum class CondOp(val opcode: Opcode, val argc: Int) {
      isNull(Opcode.IFNULL, 1), isNotNull(Opcode.IFNONNULL, 1),
      isGt0(Opcode.IFGT, 1), isGe0(Opcode.IFGE, 1),
      isEq0(Opcode.IFEQ, 1), isNeq0(Opcode.IFNE, 1)
    }
    
    override val type: ClassDesc = ConstantDescs.CD_boolean
    
    fun invokeArgs(builder: CodeBuilderWrapper) {
      // TODO: what is the order of argument? Note that `ifThenElse` reverses the condition
      args.view().reversed().forEach { it.invoke(builder) }
    }
    
    override fun invoke(p1: CodeBuilderWrapper) {
      invokeArgs(p1)
      p1.builder.ifThenElse(opcode.opcode, {
        it.iconst_1()
      }, {
        it.iconst_0()
      })
    }
  }
  
  @get:Contract(pure = true)
  val self: ExprCont by lazy {
    if (hasThis) ExprCont(inClassFile.classData.descriptor, thisRef) else {
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
      +get()
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
    
    +get()
    builder.iconst(idx)
    value.invoke(this@CodeBuilderWrapper)
    builder.arrayStoreInstruction(ty.typeKind)
  }
  
  fun mkArray(type: ClassDesc, length: Int): ExprCont {
    assert(length >= 0)
    val kind = type.typeKind
    if (kind === TypeKind.VoidType) {
      throw IllegalArgumentException("array of void")
    }
    
    return ExprCont(type.arrayType(1)) {
      builder.iconst(length)
      if (kind !== TypeKind.ReferenceType) {
        builder.newarray(kind)
      } else {
        builder.anewarray(type)
      }
    }
  }
  
  fun mkArray(type: ClassDesc, length: Int, initializer: ImmutableSeq<ExprCont>): ExprCont {
    assert(initializer.size() == length)
    val arr = mkArray(type, length)
    return ExprCont(arr.type) {
      +arr
      initializer.forEachIndexed { idx, element ->
        builder.dup()
        // position
        builder.iconst(idx)
        // value
        +element
        // store
        builder.arrayStoreInstruction(arr.type.typeKind)
      }
    }
  }
  
  /// endregion local variable helper
  
  /// region lambda helper
  
  fun MethodRef.lambda(vararg captures: ExprCont, handler: LambdaCodeCont): ExprCont {
    val entry = inClassFile.makeLambda(
      this,
      ImmutableSeq.from(captures.map { it.type }),
      handler
    )
    
    return ExprCont(this.owner) {
      ImmutableSeq.from(captures).view().reversed().forEach {
        it.invoke(this@CodeBuilderWrapper)
      }
      
      builder.invokedynamic(entry)
    }
  }
  
  /// endregion lambda helper
  
  /// region if helper
  
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
  
  fun ifInstanceOfThen(expr: ExprCont, type: ClassDesc, thenBlock: CodeBuilderWrapper.(Variable) -> Unit): IfThenBlock {
    // it is harmless that we occupy one variable slot before the code generation
    val cache = let(expr.type)
    val cachedExpr = ExprCont(expr.type) {
      cache.set(expr)
      +cache
    }
    
    // if ((cache = expr) instanceof type) {
    return ifThen(cachedExpr.instanceof(type)) {
      val casted = let(type)
      casted.set(cache.cast(type))
      // var casted = (type) cache;
      thenBlock.invoke(this, casted)
    }
    // }
  }
  
  /// endregion if helper
  
  companion object {
    val thisRef: CodeCont = { builder.aload(0) }
    val nilRef: CodeCont = { builder.aconst_null() }
    val trueRef: CodeCont = { builder.iconst(1) }
    val falseRef: CodeCont = { builder.iconst(0) }
    
    val ja: ExprCont = ExprCont(ConstantDescs.CD_boolean, trueRef)
    val nein: ExprCont = ExprCont(ConstantDescs.CD_boolean, falseRef)
    
    fun ExprCont(type: ClassDesc, cont: CodeCont): ExprCont {
      return DefaultExprCont(type, cont)
    }
  }
}
