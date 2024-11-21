package org.aya.tool.classfile

import java.util.function.Function

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
