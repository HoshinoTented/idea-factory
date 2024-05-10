import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.concrete.Expr
import com.github.hoshinotented.syntax.core.AppTerm
import com.github.hoshinotented.util.Nested
import kala.collection.immutable.ImmutableSeq

fun pi(param0: Pair<String, Expr>, vararg params: Pair<String, Expr>, last: Expr): Expr.Pi {
  val allParam = ImmutableSeq.from(params).view().prepended(param0).map {
    Expr.Param(FreeBinding(it.first), it.second)
  }
  
  return Nested.build(allParam, last, Expr::Pi)
}

fun lam(param: String, body: (Expr) -> Expr): Expr.Lam {
  return Expr.Lam(FreeBinding(param), body.invoke(ref(param)))
}

fun app(f: Expr, vararg args: Expr): Expr {
  return args.fold(f) { acc, expr ->
    Expr.App(f, expr)
  }
}

fun ref(name: String): Expr.RawRef {
  return Expr.RawRef(name)
}

fun bind(name: String, ty: Expr, definedAs: Expr): Expr.Let.Bind {
  return Expr.Let.Bind(FreeBinding(name), ty, definedAs)
}

fun let(bind: Expr.Let.Bind, body: (Expr) -> Expr): Expr.Let {
  return Expr.Let(bind, body.invoke(ref(bind.bind.name)))
}

val True: Expr = Expr.Bool(true)
val False: Expr = Expr.Bool(false)
val BoolTy: Expr = Expr.BoolTy
