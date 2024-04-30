import com.github.hoshinotented.syntax.concrete.Expr
import com.github.hoshinotented.syntax.core.AppTerm
import com.github.hoshinotented.syntax.core.BoolTerm
import com.github.hoshinotented.syntax.core.BoolTyTerm
import com.github.hoshinotented.tyck.WHNormalizer
import org.junit.Test
import kotlin.test.assertEquals

class Tests {
  @Test
  fun test0() {
    val wellTyped = check(
      lam("A", lam("a", lam("b", ref("b")))),
      pi("A" to Expr.Type, "a" to ref("A"), "b" to ref("A"), last = ref("A"))
    )
    
    val app = AppTerm(AppTerm(AppTerm(wellTyped, BoolTyTerm), BoolTerm(true)), BoolTerm(false))
    val nf = WHNormalizer(app)
    assertEquals(BoolTerm(false), nf)
  }
}