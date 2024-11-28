package org.aya.tool.classfile

import kala.collection.Map
import kala.collection.mutable.MutableMap
import java.nio.file.Files
import java.nio.file.Path

interface ClassOutput {
  companion object {
    const val SUFFIX_CLASS = ".class"
  }
  
  /**
   * All bytecode output of one "java" file,
   * class name/file name are the keys and the bytecode is the value
   */
  val outputs: Map<String, ByteArray>
  
  /**
   * Write all bytecode output to the directory [baseDir].
   */
  fun writeTo(baseDir: Path) {
    outputs.forEach { cd, ba ->
      Files.write(baseDir.resolve(cd + SUFFIX_CLASS), ba)
    }
  }
}

class DefaultClassOutput(
  override val outputs: MutableMap<String, ByteArray>
) : ClassOutput {
  fun addOutput(className: String, output: ByteArray) {
    val exists = outputs.put(className, output).isNotEmpty
    if (exists) {
      throw IllegalStateException("Duplicate class output: $className")
    }
  }
}