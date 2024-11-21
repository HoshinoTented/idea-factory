package org.aya.tool.classfile

import kala.collection.Seq
import kala.collection.immutable.ImmutableArray
import java.lang.classfile.CodeBuilder
import java.lang.classfile.TypeKind
import java.lang.constant.DirectMethodHandleDesc
import java.lang.reflect.AccessFlag

class CodeBuilderWrapper(val builder: CodeBuilder, val pool: VariablePool, private val hasThis: Boolean) {
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
  ): CodeBuilderWrapper {
    return invoke(InvokeKind.Interface, theObject, theMethod, args)
  }
  
  fun invokevirtual(
    theObject: CodeCont,
    theMethod: MethodData,
    args: Seq<CodeCont>,
  ): CodeBuilderWrapper {
    return invoke(InvokeKind.Virtual, theObject, theMethod, args)
  }
  
  fun invokespecial(
    theObject: CodeCont,
    theMethod: MethodData,
    args: Seq<CodeCont>,
  ): CodeBuilderWrapper {
    return invoke(InvokeKind.Special, theObject, theMethod, args)
  }
  
  fun invokestatic(
    theMethod: MethodData,
    args: Seq<CodeCont>,
  ): CodeBuilderWrapper {
    return invoke(InvokeKind.Static, null, theMethod, args)
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
  
  data class MethodRef(val data: MethodData, val obj: CodeBuilderWrapper.() -> Unit)
  
  fun MethodData.of(obj: CodeBuilderWrapper.() -> Unit): MethodRef {
    assert(!this.flags.has(AccessFlag.STATIC))
    return MethodRef(this@of, obj)
  }
  
  fun MethodRef.invoke(vararg args: CodeCont): CodeCont {
    val argSeq = ImmutableArray.Unsafe.wrap<CodeCont>(args)
    return when (data.kind()) {
      DirectMethodHandleDesc.Kind.VIRTUAL -> {
        { invokespecial(obj, data, argSeq) }
      }
      
      DirectMethodHandleDesc.Kind.INTERFACE_VIRTUAL -> {
        { invokeinterface(obj, data, argSeq) }
      }
      
      DirectMethodHandleDesc.Kind.SPECIAL,
      DirectMethodHandleDesc.Kind.INTERFACE_SPECIAL,
        -> {
        { invokespecial(obj, data, argSeq) }
      }
      
      else -> TODO("unreachable")
    }
  }
  
  /// endregion method helper
  
  /// region field helper
  
  /**
   * A "reference" to a field of certain object, the instruction is wrote until the command is given.
   */
  data class FieldRef(val data: FieldData, val obj: CodeBuilderWrapper.() -> Unit)
  
  fun FieldData.of(obj: CodeBuilderWrapper.() -> Unit): FieldRef {
    assert(!this.flags.has(AccessFlag.STATIC))
    return FieldRef(this@of, obj)
  }
  
  fun FieldData.set(value: CodeCont) {
    value.invoke(this@CodeBuilderWrapper)
    builder.putstatic(this.owner, this.name, this.returnType)
  }
  
  fun FieldRef.set(value: CodeCont) {
    obj.invoke(this@CodeBuilderWrapper)
    value.invoke(this@CodeBuilderWrapper)
    with(this.data) {
      builder.putfield(this.owner, this.name, this.returnType)
    }
  }
  
  /// endregion field helper
  
  /// region easy argument
  
  fun arg(nth: Int): Int {
    return nth + if (hasThis) 1 else 0
  }
  
  fun aarg(nth: Int): CodeCont = argInstruction(TypeKind.ReferenceType, nth)
  fun iarg(nth: Int): CodeCont = argInstruction(TypeKind.IntType, nth)
  
  fun argInstruction(kind: TypeKind, nth: Int): CodeCont = {
    builder.loadInstruction(kind, arg(nth))
  }
  
  val self: CodeCont = if (hasThis) thisRef else {
    throw IllegalStateException("static")
  }
  
  /// endregion easy argument
  
  companion object {
    fun aref(slot: Int): CodeCont {
      return { builder.aload(slot) }
    }
    
    val thisRef: CodeCont = { builder.aload(0) }
  }
}
