package org.aya.tool.classfile

import kala.collection.Seq
import kala.collection.immutable.ImmutableArray
import kala.collection.immutable.ImmutableSeq
import org.jetbrains.annotations.Contract
import java.lang.classfile.CodeBuilder
import java.lang.classfile.TypeKind
import java.lang.constant.ClassDesc
import java.lang.constant.DirectMethodHandleDesc
import java.lang.reflect.AccessFlag

class CodeBuilderWrapper constructor(
  private val inClassFile: ClassBuilderWrapper,
  val builder: CodeBuilder,
  val pool: VariablePool,
  private val hasThis: Boolean
) {
  /// region invoke
  private enum class InvokeKind {
    Interface,
    Virtual,
    Static,
    Special
  }
  
  fun invokeinterface(
    theObject: CodeCont,
    theMethod: MethodData,
    args: Seq<CodeCont>,
  ) {
    invoke(InvokeKind.Interface, theObject, theMethod, args)
  }
  
  fun invokevirtual(
    theObject: CodeCont,
    theMethod: MethodData,
    args: Seq<CodeCont>,
  ) {
    invoke(InvokeKind.Virtual, theObject, theMethod, args)
  }
  
  fun invokespecial(
    theObject: CodeCont,
    theMethod: MethodData,
    args: Seq<CodeCont>,
  ) {
    invoke(InvokeKind.Special, theObject, theMethod, args)
  }
  
  fun invokestatic(
    theMethod: MethodData,
    args: Seq<CodeCont>,
  ) {
    invoke(InvokeKind.Static, null, theMethod, args)
  }
  
  private fun invoke(
    invokeKind: InvokeKind,
    theObject: CodeCont?,
    theMethod: MethodData,
    args: Seq<CodeCont>,
  ): CodeBuilderWrapper {
    theObject?.invoke(this)
    args.view().reversed().forEach { f ->
      f.invoke(this)
    }
    
    val owner = theMethod.inClass
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
    assert(!isStatic)
    return MethodRef(this@of, obj)
  }
  
  fun MethodData.invoke(vararg args: CodeCont): ExprCont {
    assert(isStatic)
    val argSeq = ImmutableArray.Unsafe.wrap<CodeCont>(args)
    return ExprCont(this.signature.returnType()) {
      invokestatic(this@invoke, argSeq)
    }
  }
  
  @Contract(pure = true)
  fun MethodRef.invoke(vararg args: CodeCont): ExprCont {
    val argSeq = ImmutableArray.Unsafe.wrap<CodeCont>(args)
    val cont: CodeCont = when (data.kind()) {
      DirectMethodHandleDesc.Kind.VIRTUAL -> {
        { invokevirtual(obj, data, argSeq) }
      }
      
      DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL -> {
        { invokeinterface(obj, data, argSeq) }
      }
      DirectMethodHandleDesc.Kind.SPECIAL,
      DirectMethodHandleDesc.Kind.INTERFACE_SPECIAL -> {
        { invokespecial(obj, data, argSeq) }
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
  
  fun MethodData.nu(vararg args: CodeCont): ExprCont = ExprCont(this.inClass) {
    builder.new_(this@nu.inClass)
    // invoke constructor
    invokespecial({ builder.dup() }, this@nu, ImmutableArray.Unsafe.wrap(args))
  }
  
  fun ret(value: ExprCont? = null) {
    if (value == null) {
      builder.return_()
    } else {
      val ty = assertValidType(value.type)
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
  
  /// region easy argument
  
  /**
   * A [CodeCont] that push an expression to the stack, with type information [type].
   * Note that [type] may not be a valid type, e.g. `void`.
   */
  data class ExprCont(val type: ClassDesc, val cont: CodeCont) : (CodeBuilderWrapper) -> Unit {
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
  
  fun subscoped(newPool: VariablePool): CodeBuilderWrapper {
    return CodeBuilderWrapper(inClassFile, builder, newPool, hasThis)
  }
  
  inline fun <R> subscoped(block: CodeBuilderWrapper.() -> R): R {
    return this.pool.subscoped {
      subscoped(it).block()
    }
  }
  
  /// endregion easy argument
  
  /// region lambda helper
  
  fun MethodData.lambda(vararg captures: ExprCont, handler: LambdaCodeCont): ExprCont {
    val entry = inClassFile.makeLambda(
      this,
      ImmutableSeq.from(captures.map { it.type }),
      handler
    )
    
    return ExprCont(this.inClass) {
      ImmutableSeq.from(captures).view().reversed().forEach {
        it.cont.invoke(this@CodeBuilderWrapper)
      }
      
      builder.invokedynamic(entry)
    }
  }
  
  /// endregion
  
  companion object {
    val thisRef: CodeCont = { builder.aload(0) }
  }
}
