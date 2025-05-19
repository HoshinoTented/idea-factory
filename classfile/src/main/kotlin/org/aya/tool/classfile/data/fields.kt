package org.aya.tool.classfile.data

import org.aya.tool.classfile.AccessFlagSet
import java.lang.classfile.ClassBuilder
import java.lang.constant.ClassDesc

@JvmRecord
data class FieldData(
  val owner: ClassDesc,
  val flags: AccessFlagSet,
  val returnType: ClassDesc,
  val name: String,
) {
  fun build(cb: ClassBuilder) {
    cb.withField(name, returnType) { fb ->
      fb.withFlags(*flags.toArray())
    }
  }
}