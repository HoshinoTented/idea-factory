package org.aya.tool.classfile

class VariablePool
@JvmOverloads
constructor(private var counter: Int = -1) {
  fun acquire(): Int {
    return ++counter
  }
  
  fun clone(): VariablePool {
    return VariablePool(counter)
  }
  
  inline fun <R> subscoped(block: (VariablePool) -> R): R {
    return block.invoke(clone())
  }
}
