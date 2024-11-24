package org.aya.tool.classfile

import java.lang.classfile.CodeBuilder

fun CodeBuilder.iconst(i: Int) {
  when (i) {
    0 -> iconst_0()
    1 -> iconst_1()
    2 -> iconst_2()
    3 -> iconst_3()
    4 -> iconst_4()
    5 -> iconst_5()
    -1 -> iconst_m1()
    else -> ldc(constantPool().intEntry(i))
  }
}