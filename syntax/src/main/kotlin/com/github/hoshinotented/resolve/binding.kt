package com.github.hoshinotented.resolve

sealed interface Binding {
}

data class FreeBinding(val name: String) : Binding {
  override fun equals(other: Any?): Boolean {
    return this === other
  }
  
  override fun toString(): String {
    return name
  }
  
  override fun hashCode(): Int {
    return System.identityHashCode(this)
  }
}