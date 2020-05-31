package co.moelten.splity

import com.youneedabudget.client.createBudgetsApi
import kotlinx.coroutines.runBlocking

fun main() {
  runBlocking {
    val budgetsApi = createBudgetsApi()

    val something = budgetsApi.getBudgets(false)

    println(something.data.budgets)
  }
}
