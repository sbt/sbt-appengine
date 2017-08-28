package simple

import javax.jdo.annotations._
import javax.jdo._
import com.google.appengine.api.datastore.Key
import scala.annotation.meta.field

@PersistenceCapable
case class Counter(
  @(PrimaryKey @field)
  @(Persistent @field)(valueStrategy = IdGeneratorStrategy.IDENTITY)
  var key: Key,
  @(Persistent @field)
  var count: Int)

// https://cloud.google.com/appengine/docs/standard/java/datastore/jdo/creatinggettinganddeletingdata
object CounterAdapter {
  def get(key: Key): Option[Counter] = {
    val pm = PMF.instance.getPersistenceManager
    try {
      Some(pm.detachCopy(pm.getObjectById(classOf[Counter], key)))
    } catch {
      case e: JDOObjectNotFoundException =>
        None
      case e: Exception => throw e
    } finally {
      pm.close
    }
  }

  def save(value: Counter): Unit = {
    val pm = PMF.instance.getPersistenceManager
    try {
      pm.makePersistent(value)
    } finally {
      pm.close
    }
  }
}
