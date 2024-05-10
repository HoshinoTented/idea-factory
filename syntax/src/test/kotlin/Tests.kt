import com.github.hoshinotented.syntax.concrete.Expr
import com.github.hoshinotented.syntax.core.AppTerm
import com.github.hoshinotented.syntax.core.BoolTerm
import com.github.hoshinotented.syntax.core.BoolTyTerm
import com.github.hoshinotented.syntax.core.Term
import com.github.hoshinotented.tyck.WHNormalizer
import org.junit.Test
import kotlin.test.assertEquals

class Tests {
  fun whnf(term: Term): Term {
    return WHNormalizer().whnf(term)
  }
  
  @Test
  fun test0() {
    val wellTyped = check(
      lam("A") { A ->
        lam("a") { a ->
          lam("b") { b -> b }
        }
      },
      pi("A" to Expr.Type, "a" to ref("A"), "b" to ref("A"), last = ref("A"))
    )
    
    val app = AppTerm(AppTerm(AppTerm(wellTyped, BoolTyTerm), BoolTerm(true)), BoolTerm(false))
    val nf = whnf(app)
    assertEquals(BoolTerm(false), nf)
  }
  
  @Test
  fun let0() {
    val term = check(lam("a") { a ->
      // let b : Bool -> Bool := \ a. a
      let(bind("b", pi("_" to BoolTy, last = BoolTy), lam("a") { a -> a })) { b ->
        // in b a
        app(b, a)
      }
    }, pi("_" to BoolTy, last = BoolTy))
    
    val app = AppTerm(term, BoolTerm(true))
    val nf = whnf(app)
    assertEquals(BoolTerm(true), nf)
  }
  
  @Test
  fun letUnify0() {
    val ty = BoolTy
    val lhs = check(let(bind("f", pi("_" to BoolTy, last = BoolTy), lam("a") { a -> a })) { f ->
      app(f, True)
    }, ty)
    val rhs = check(let(bind("b", BoolTy, False)) { _ -> True }, ty)
    
    mkExprTycker().unifyReport(lhs, rhs, null)
  }
}