package org.aya.tool.classfile

import kala.collection.Map
import kala.collection.mutable.MutableMap
import java.nio.file.Files
import java.nio.file.Path

interface ClassOutput {
  companion object {
    const val SUFFIX_CLASS = ".class"
  }
  
  val outputs: Map<String, ByteArray>
  
  fun writeTo(baseDir: Path) {
    outputs.forEach { cd, ba ->
      Files.write(baseDir.resolve(cd + SUFFIX_CLASS), ba)
    }
  }
}

class DefaultClassOutput(
  override val outputs: MutableMap<String, ByteArray>
) : ClassOutput