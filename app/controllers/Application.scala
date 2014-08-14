package controllers


import akka.pattern.{CircuitBreakerOpenException, CircuitBreaker}
import play.api.libs.concurrent.Akka
import play.api.mvc.{Results, SimpleResult, Action, Controller}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Random
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

/**
 * Open four tabs and call async or sync, after a while it will start to fail-fast
 */
object Application extends Controller {

  def index = Action {
    Ok(views.html.index("Your new application is ready."))
  }

  val breaker = new CircuitBreaker(Akka.system.scheduler, 2, 10 seconds, 1 minute)

  def async() = Action.async {

    breaker.withCircuitBreaker(Future(riskySyncOp())).fallbackTo(Future.successful("Open circuit"))
  }

  def sync() = Action {

    try{
      breaker.withSyncCircuitBreaker(riskySyncOp())
    } catch {
      case x: CircuitBreakerOpenException => "Open circuit"
    }

  }

  private def riskySyncOp(): String = {
    val duration = 12
    println(s"Waiting $duration")

    Thread.sleep(duration * 1000)

    s"hello world took $duration seconds"
  }

  implicit def string2SimpleResult(any: String): SimpleResult = Results.Ok(any)

  implicit def anyFuture2SimpleResult(anyFuture: Future[String]): Future[SimpleResult] = {
    anyFuture.map(any => implicitly[SimpleResult](any))
  }

}