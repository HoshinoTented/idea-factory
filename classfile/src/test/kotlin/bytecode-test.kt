import kala.collection.Seq
import org.aya.tool.classfile.ClassData
import org.aya.tool.classfile.MD_Object_init
import org.aya.tool.classfile.MD_toString
import org.aya.tool.classfile.public
import org.junit.jupiter.api.Test
import java.lang.classfile.ClassFile
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.nio.file.Files
import java.nio.file.Path

class BytecodeTest {
  companion object {
    val GEN_DIR: Path = Path.of("src", "test", "gen")
  }
  
  @Test
  fun test0() {
    val Object_toString = MD_toString(ConstantDescs.CD_Object)
    
    val bytecode = ClassData(ClassDesc.of("SomeMain")).build(ClassFile.of()) {
      val field_foo = public().field(ConstantDescs.CD_String, "foo")
      
      public().constructor(
        Seq.of(ConstantDescs.CD_Object),
        MD_Object_init,
        Seq.empty()
      ) {
        val arg0 = aarg(0)
        
        field_foo.of(self).set(Object_toString.of(arg0).invoke())
      }
    }
    
    Files.write(GEN_DIR.resolve("SomeMain.class"), bytecode)
  }
}