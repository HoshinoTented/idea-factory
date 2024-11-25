package org.aya.tool.classfile

import java.lang.classfile.TypeKind
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs

/**
 * Make [ClassDesc] from [Class]
 */
fun Class<*>.asDesc(): ClassDesc {
  return ClassDesc.ofDescriptor(descriptorString())
}

/**
 * Check whether the type [desc] is a valid type, that is, not `void`, but `Void` is.
 */
fun assertValidType(desc: ClassDesc): ClassDesc {
  assert(desc.descriptorString() != ConstantDescs.CD_void.descriptorString()) {
    "void"
  }
  
  return desc
}

// TODO: check whether the standard library provides similar function
//       if not, try build an enum that stores the relationships like `boolean` and `java.lang.Boolean`
fun isBoolean(desc: ClassDesc): Boolean {
  return desc.descriptorString() == ConstantDescs.CD_boolean.descriptorString()
          || desc.descriptorString() == ConstantDescs.CD_Boolean.descriptorString()
}

fun isInteger(desc: ClassDesc): Boolean {
  return desc.descriptorString() == ConstantDescs.CD_int.descriptorString()
          || desc.descriptorString() == ConstantDescs.CD_Integer.descriptorString()
}

/**
 * Check whether two types match, fail if mismatch, true if match and not `void`, false if both `void`.
 */
fun assertTypeMatch(lhs: ClassDesc, rhs: ClassDesc): Boolean {
  val lhsDesc = lhs.descriptorString()
  val rhsDesc = rhs.descriptorString()
  
  assert(lhsDesc == rhsDesc) {
    """
        type not match:
          lhs: $lhsDesc
          rhs: $rhsDesc
      """.trimIndent()
  }
  
  return lhs.descriptorString() != ConstantDescs.CD_void.descriptorString()
}

val ClassDesc.typeKind: TypeKind get() = TypeKind.fromDescriptor(descriptorString())