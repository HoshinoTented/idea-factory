package org.aya.tool.classfile

import java.lang.classfile.CodeBuilder
import java.lang.constant.ClassDesc

/**
 * A continuation about instruction generation.
 * When used as expression, i.e. arguments,
 * the continuation must push/load a value to the stack.
 */
typealias CodeCont = CodeBuilderWrapper.() -> Unit

fun Class<*>.asDesc(): ClassDesc {
  val name: String = this.getName()
  val first = name[0]
  return if (first == '[' || first == 'L') {
    // in case of array Class
    ClassDesc.ofDescriptor(name)
  } else {
    ClassDesc.of(name)
  }
}

object ClassDescUtils {
  private const val CDS_void: String = "V"
  private const val CDS_byte: String = "B"
  private const val CDS_short: String = "S"
  private const val CDS_int: String = "I"
  private const val CDS_long: String = "J"
  private const val CDS_char: String = "C"
  private const val CDS_boolean: String = "Z"
  private const val CDS_float: String = "F"
  private const val CDS_double: String = "D"
  
  fun store(builder: CodeBuilder, type: ClassDesc, to: Int) {
    assert(to >= 0)
    if (type.isPrimitive) {
      when (type.descriptorString()) {
        CDS_void -> {}
        CDS_byte, CDS_short, CDS_char, CDS_int, CDS_boolean -> builder.istore(to)
        CDS_long -> builder.lstore(to)
        CDS_float -> builder.fstore(to)
        CDS_double -> builder.dstore(to)
        else -> throw RuntimeException("unreachable")
      }
    } else {
      builder.astore(to)
    }
  }
}