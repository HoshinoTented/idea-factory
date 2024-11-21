package org.aya.tool.classfile

import java.lang.classfile.AccessFlags
import java.lang.constant.ConstantDescs
import java.lang.constant.MethodTypeDesc
import java.lang.invoke.LambdaMetafactory

/**
 * @see LambdaMetafactory.metafactory
 */
val MTD_LambdaMetafactory_metafactory = MethodTypeDesc.of(
  ConstantDescs.CD_CallSite,
  ConstantDescs.CD_MethodHandles_Lookup,
  ConstantDescs.CD_String,
  ConstantDescs.CD_MethodType,
  ConstantDescs.CD_MethodType,
  ConstantDescs.CD_MethodHandle,
  ConstantDescs.CD_MethodType,
)

const val MAIN_NAME: String = "main"

val MD_Object_new: MethodData = MethodData(
  ConstantDescs.CD_Object,
  ConstantDescs.INIT_NAME,
  AccessFlags.ofMethod(public().mask()),
  ConstantDescs.MTD_void,
  false
)