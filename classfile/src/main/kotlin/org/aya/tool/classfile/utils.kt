package org.aya.tool.classfile

import java.lang.classfile.CodeBuilder
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs

/**
 * A continuation about instruction generation.
 * When used as expression, i.e. arguments,
 * the continuation must push/load a value to the stack.
 */
typealias CodeCont = CodeBuilderWrapper.() -> Unit

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

/**
 * Check whether the type [desc] is a valid type, that is, not `void`
 */
fun assertValidType(desc: ClassDesc): ClassDesc {
  assert(desc.descriptorString() == ConstantDescs.CD_void.descriptorString()) {
    "void"
  }
  
  return desc
}