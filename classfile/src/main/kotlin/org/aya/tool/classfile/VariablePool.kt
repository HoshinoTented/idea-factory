package org.aya.tool.classfile

interface VariablePool {
  fun acquire(): Int
  fun clone(): VariablePool
}

inline fun <R> VariablePool.subscoped(block: (VariablePool) -> R): R {
  return block.invoke(clone())
}

class DefaultVariablePool(private var counter: Int = -1) : VariablePool {
  override fun acquire(): Int {
    return ++counter
  }
  
  override fun clone(): VariablePool {
    return DefaultVariablePool(counter)
  }
}

object DummyVariablePool : VariablePool {
  override fun acquire(): Int {
    throw UnsupportedOperationException("DummyVariablePool")
  }
  
  override fun clone(): VariablePool {
    throw UnsupportedOperationException("DummyVariablePool")
  }
}