import java.io.PrintStream
import java.lang.reflect.ParameterizedType
import java.util.function.BiConsumer

fun <T : Any> foo(f: T) {
  val kfInterface = f::class.java.genericInterfaces[0]
  
  assert(kfInterface is ParameterizedType) {
    "Use form Owner::method"
  }
  
  kfInterface as ParameterizedType
}

fun main() {
  foo<PrintStream.(Int) -> Unit>(PrintStream::println)
}