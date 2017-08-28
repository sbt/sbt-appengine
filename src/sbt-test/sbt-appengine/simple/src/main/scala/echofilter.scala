package simple

import unfiltered.filter.Plan
import unfiltered.request._
import unfiltered.response._
import unfiltered.request.{Path => UFPath}
import com.google.appengine.api.datastore.{Key, KeyFactory}

class EchoFilter extends Plan {
  override val intent: Plan.Intent = {
    case GET(UFPath(Seg(what :: Nil)) & Params(params0)) =>
      val key = KeyFactory.createKey(classOf[Counter].getSimpleName, what)
      val counter = CounterAdapter.get(key) getOrElse new Counter(key, 0)
      val inc = new Counter(key, counter.count + 1)
      CounterAdapter.save(inc)
      ResponseString(what + inc.count.toString + "!")
  }
}
