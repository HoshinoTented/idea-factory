package com.github.hoshinotented.tyck

import com.github.hoshinotented.resolve.FreeBinding
import com.github.hoshinotented.syntax.core.*
import com.github.hoshinotented.tyck.ctx.LocalContext
import com.github.hoshinotented.tyck.ctx.LocalDefinitions

class Conversion(
  localCtx: LocalContext,
  localDefs: LocalDefinitions,
) : AbstractTycker<Conversion>(localCtx, localDefs) {
  override fun set(newCtx: LocalContext, newDefs: LocalDefinitions): Conversion {
    return Conversion(newCtx, newDefs)
  }
  
  fun check(preLhs: Term, preRhs: Term, type: Term?): Boolean {
    val result = checkApproximate(preLhs, preRhs)
    if (result != null) return if (type != null) {
      checkUntyped(result, type) != null
    } else true
    
    val lhs = whnf(preLhs)
    val rhs = whnf(preRhs)
    //  we no need to run the following code, since it will be done in checkUntyped
    //  result = checkApproximate(lhs, rhs)
    
    return loadLet(lhs, rhs) { lhsBody, rhsBody ->
      if (type != null) checkTyped(lhsBody, rhsBody, whnf(type))
      else checkUntyped(lhsBody, rhsBody) != null
    }
  }
  
  private fun checkTyped(lhs: Term, rhs: Term, type: Term): Boolean {
    return when (type) {
      is PiTerm -> when {
        lhs is LamTerm && rhs is LamTerm -> {
          val binding = FreeBinding(type.param.name)
          val ref = FreeRefTerm(binding)
          val pbody = type.last.instantiate(ref)
          val lbody = lhs.body.instantiate(ref)
          val rbody = rhs.body.instantiate(ref)
          
          subscoped {
            localCtx[binding] = type.param.type
            check(lbody, rbody, pbody)
          }
        }
        lhs is LamTerm -> checkLam(lhs, rhs, type)
        rhs is LamTerm -> checkLam(rhs, lhs, type)
        else -> check(lhs, rhs, null)
      }
      
      is LamTerm -> throw IllegalArgumentException("Lambda is never a type")
      Type -> if (lhs is Formation) checkType(lhs, rhs) != null else false
      else -> {
        val result = checkUntyped(lhs, rhs) ?: return false
        check(type, result, null)
      }
    }
  }
  
  /**
   * @return a whnf type if success
   */
  private fun checkUntyped(lhs: Term, rhs: Term): Term? {
    return when (lhs) {
      is Formation -> checkType(lhs, rhs)
      // see checkApproximate
      is AppTerm -> if (rhs is AppTerm) {
        checkApp(lhs, rhs)
      } else null
      
      is FreeRefTerm -> if (rhs is FreeRefTerm && lhs.ref === rhs.ref) whnf(localCtx[lhs.ref]) else null
      is BoolTerm -> if (rhs is BoolTerm && lhs.value == rhs.value) {
        BoolTyTerm
      } else null
      
      // explicit for hint!
      is LetTerm -> noRules(lhs, rhs)
      else -> noRules(lhs, rhs)
    }
  }
  
  private fun checkType(lhs: Formation, rhs: Term): Term? {
    return when (lhs) {
      is PiTerm -> if (rhs is PiTerm) {
        // compare param type
        val paramResult = check(lhs.param.type, rhs.param.type, Type)
        if (!paramResult) return null
        
        val binding = FreeBinding(lhs.param.name)
        val ref = FreeRefTerm(binding)
        val lbody = lhs.last.instantiate(ref)
        val rbody = rhs.last.instantiate(ref)
        
        val bodyResult = subscoped {
          localCtx[binding] = lhs.param.type
          check(lbody, rbody, Type)
        }
        
        if (bodyResult) Type else null
      } else null
      
      BoolTyTerm -> if (rhs === BoolTyTerm) return Type else null
      Type -> if (rhs === Type) return Type else null
    }
  }
  
  private fun checkApproximate(lhs: Term, rhs: Term): Term? {
    return if (lhs is AppTerm && rhs is AppTerm) checkApp(lhs, rhs) else null
  }
  
  private fun checkApp(lapp: AppTerm, rapp: AppTerm): Term? {
    val head = checkUntyped(lapp.f, rapp.f) ?: return null
    return if (head is PiTerm) {
      val bodyResult = check(lapp.a, rapp.a, head.param.type)
      if (bodyResult) {
        head.last.instantiate(lapp.a)
      } else null
    } else null
  }
  
  private fun checkLam(lhs: LamTerm, rhs: Term, type: PiTerm): Boolean {
    val binding = FreeBinding(type.param.name)
    val ref = FreeRefTerm(binding)
    val pbody = type.last.instantiate(ref)
    val lbody = lhs.body.instantiate(ref)
    val rbody = AppTerm(rhs, ref)
    
    return subscoped {
      localCtx[binding] = type.param.type
      check(lbody, rbody, pbody)
    }
  }
  
  private fun checkLetBind(lhs: LetTerm.Bind, rhs: LetTerm.Bind): Boolean {
    // check type
    if (!check(lhs.name.type, rhs.name.type, null)) return false
    // check definedAs
    return check(lhs.definedAs, rhs.definedAs, lhs.name.type)
  }
  
  private fun checkLet(lhs: LetTerm, rhs: Term, type: Term, checker: (Term, Term, Term) -> Boolean): Boolean {
    return load(lhs) {
      checker(rhs, it, type)
    }
  }
  
  private fun loadLet(lhs: Term, rhs: Term, checker: Conversion.(Term, Term) -> Boolean): Boolean {
    return if (lhs is LetTerm) {
      load(lhs) { lhsBody ->
        if (rhs is LetTerm) {
          load(rhs) { rhsBody ->
            checker(lhsBody, rhsBody)
          }
        } else checker(lhsBody, rhs)
      }
    } else if (rhs is LetTerm) {
      load(rhs) { rhsBody ->
        checker(lhs, rhsBody)
      }
    } else checker(lhs, rhs)
  }
  
  private fun noRules(lhs: Term, rhs: Term): Nothing {
    throw IllegalStateException("No rule for $lhs")
  }
}