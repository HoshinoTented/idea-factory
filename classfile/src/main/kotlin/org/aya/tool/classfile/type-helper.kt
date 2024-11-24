package org.aya.tool.classfile

import java.lang.classfile.TypeKind
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.lang.reflect.Type
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaType
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.jvm.jvmName

/**
 * Make [ClassDesc] from [Class]
 */
fun Class<*>.asDesc(): ClassDesc {
  val name: String = this.getName()
  val first = name[0]
  
  return if (first == '[' || first == 'L') {
    // in case of array Class
    ClassDesc.ofDescriptor(name.replace('.', '/'))
  } else {
    ClassDesc.of(name)
  }
}

fun <R> KFunction<R>.asDesc(): MethodTypeDesc {
  val params = parameters.map { it.type.jvmErasure.java.asDesc() }
  val ret = returnType.jvmErasure.java.asDesc()
  return MethodTypeDesc.of(ret, params)
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
 * Check whether two type matches, fail if mismatch, true if matches and not `void`, false if both `void`
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