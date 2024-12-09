import kala.collection.Seq
import kala.collection.immutable.ImmutableSeq
import org.aya.tool.classfile.*
import org.junit.jupiter.api.Test
import java.io.PrintStream
import java.lang.classfile.AccessFlags
import java.lang.classfile.ClassFile
import java.lang.constant.ClassDesc
import java.lang.constant.ConstantDescs
import java.lang.constant.DirectMethodHandleDesc
import java.lang.constant.MethodTypeDesc
import java.lang.reflect.AccessFlag
import java.nio.file.Path

class BytecodeTest {
  companion object {
    val GEN_DIR: Path = Path.of("src", "test", "gen")
  }
  
  @Test
  fun test0() {
    val field_System_out = FieldData(
      System::class.java.asDesc(),
      AccessFlags.ofField(public().static().final().mask()),
      PrintStream::class.java.asDesc(),
      "out"
    )
    
    val method_PrintStream_println = MethodData(
      PrintStream::class.java.asDesc(),
      "println",
      AccessFlags.ofMethod(public().mask()),
      MethodTypeDesc.of(ConstantDescs.CD_void, ConstantDescs.CD_String),
      false
    )
    
    val method_Boolean_valueOf = MethodData(
      java.lang.Boolean::class.java.asDesc(),
      "valueOf",
      AccessFlags.ofMethod(public().static().mask()),
      MethodTypeDesc.of(ConstantDescs.CD_Boolean, ConstantDescs.CD_boolean),
      false
    )
    
    val method_Object_toString = MD_toString(ConstantDescs.CD_Object)
    // Runnable#run : () -> ()
    val method_Runnable_run = MethodData(
      Runnable::class.java.asDesc(),
      "run",
      AccessFlags.ofMethod(AccessFlag.PUBLIC, AccessFlag.ABSTRACT),
      MethodTypeDesc.of(ConstantDescs.CD_void),
      true
    )
    
    val output = ClassData(ClassDesc.of("SomeMain")).build(ClassFile.of()) {
      val SomeMain = this.classData.descriptor
      builder.withVersion(64, 0)
      
      InnerClassData(public(), "ManyMain").nestedClass(ClassFile.of()) {
        defaultConstructor()
      }
      
      val field_foo = public().field(ConstantDescs.CD_String, "foo")
      
      val SomeMain_nu = public().constructor(
        MD_Object_init, ImmutableSeq.empty(),
        ConstantDescs.CD_Object
      ) { arg0 ->
        field_foo.of(self).set(method_Object_toString.of(arg0).invoke())
        val f = method_Runnable_run.lambda(self) {
          val self = it.capture(0)
          val out = field_System_out.get()
          +method_PrintStream_println.of(out).invoke(field_foo.of(self).get())
          ret()
        }
        
        +method_Runnable_run.of(f).invoke()
        ret()
      }
      
      val SomeMain_bar = public().method(ConstantDescs.CD_boolean, "bar") {
        val ref_foo = field_foo.of(self)
        val result = let(ConstantDescs.CD_boolean)
        ifInstanceOfThen(ref_foo.get(), ConstantDescs.CD_String) { realString ->
          +method_PrintStream_println.of(field_System_out.get()).invoke(realString)
          result.set(true)
        }.orElse {
          ref_foo.set(CodeBuilderWrapper.nilRef)
          result.set(false)
        }.end()
        
        ret(result)
      }
      
      main { args ->
        // var someMain = new SomeMain(new Object())
        val someMain = let(SomeMain)
        someMain.set(SomeMain_nu.nu(MD_Object_new.nu()))
        
        // var someMainResult = someMain.bar()
        val someMainResult = let(ConstantDescs.CD_boolean)
        someMainResult.set(SomeMain_bar.of(someMain).invoke())
        
        // System.out.println(
        +method_PrintStream_println.of(field_System_out.get()).invoke(
          // Boolean.valueOf(someMainResult).toString()
          method_Object_toString.of(method_Boolean_valueOf.invoke(someMainResult)).invoke()
        )
        // )
        
        ret()
      }
    }
    
    output.writeTo(GEN_DIR)
  }
  
  @Test
  fun polyMethodWithoutCast() {
    val output = ClassData(ClassDesc.of("WithoutCast")).build(ClassFile.of()) {
      this.builder.withVersion(65, 0)
      
      defaultConstructor()
      
      val id = public().static().method(ConstantDescs.CD_Object, "id", ConstantDescs.CD_Object) {
        ret(it)
      }
      
      main {
        val String_length = MethodRef(
          ConstantDescs.CD_String,
          "length",
          ImmutableSeq.empty(),
          ConstantDescs.CD_int,
          DirectMethodHandleDesc.Kind.VIRTUAL,
        )
        
        val result = let(ConstantDescs.CD_Object)
        result.set(id.invoke(aconst("114514")))
        +String_length.of(result).invoke()
        ret()
      }
    }
    
    output.writeTo(GEN_DIR)
  }
}