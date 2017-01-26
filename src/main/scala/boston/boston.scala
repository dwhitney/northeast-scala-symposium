package nescala.boston

import nescala.{ AuthorizedToken, Cached, Clock, Meetup, Store }
import unfiltered.request._
import unfiltered.request.QParams._
import unfiltered.response._
import unfiltered.Cycle
import com.redis.RedisClient

object Boston extends Templates {

  def indexPage(
    req: HttpRequest[Any],
    pathVars: Map[String, String]
  ) = req match {
    case GET(Path(Seg("2012":: Nil))) & AuthorizedToken(t) => Clock("home") {
      Store { s =>
        index(true, keynote(s), talks(s), panel(s))
      }
    }
    case GET(Path(Seg("2012" :: Nil))) => Clock("home") {
      Store { s =>
        index(false, keynote(s), talks(s), panel(s))
      }
    }
  }

  def friends(
    req: HttpRequest[Any],
    pathVars: Map[String, String]
  ) = req match {
    case GET(Path(Seg("2012" :: "friends" :: Nil))) =>
      sponsors
  }

  def api(
    req: HttpRequest[Any],
    pathVars: Map[String, String]
  ) = req match {
    case GET(Path(Seg("boston" :: "rsvps" :: event :: Nil))) => Clock("fetching rsvp list for %s" format event) {
      import org.json4s.native.JsonMethods.{ compact, render }
      JsonContent ~> ResponseString(
        compact(render(Cached.Boston.Rsvps(event))))
    }
  }

  def pannelProposals(
    req: HttpRequest[Any],
    pathVars: Map[String, String]
  ) = req match {
    case req @ GET(Path(Seg("2012" :: "panels" :: Nil))) => Redirect("/")
  }

  def talkProposals(
    req: HttpRequest[Any],
    pathVars: Map[String, String]
  ) = req match {
    case req @ GET(Path(Seg("2012" :: "talks" :: Nil))) => Redirect("/")
  }

  /*def site: unfiltered.Cycle.Intent[Any, Any] =
    (index /: Seq(talkProposals,
                  panelProposals,
                  Votes.intent,
                  Boston.api,
                  Tally.talks,
                  Tally.panels))(_ orElse _)*/

  private def mukey(of: String) = "boston:members:%s" format of



  private def talks(r: RedisClient): Seq[Map[String, String]] = {
    val Talk = """boston:talks:(.*)""".r
    r.keys("boston:talks:*") match {
      case None => Seq.empty[Map[String, String]]
      case Some(keys) =>
        ((List.empty[Map[String, String]] /: keys.flatten)(
          (a, e) => (e match {
            case t @ Talk(mid) =>
              r.hmget[String, String](t, "name", "desc", "slides", "video").map(_ + ("id" -> t)).map {
                _ ++ r.hmget[String, String](mukey(mid), "mu_name", "mu_photo", "twttr").get
              }.get :: a
            case _ => a
          }))).reverse
    }
  }

  private def keynote(r: RedisClient): Map[String, String] = {
    val Keynote = """boston:keynote:(.*)""".r
    r.keys("boston:keynote:*") match {
      case None => Map.empty[String, String]
      case Some(Some(key) :: _) =>
        key match {
          case k @ Keynote(mid) =>
            r.hmget[String, String](k, "name", "desc", "slides", "video").map(_ + ("id" -> k)).map {
              _ ++ r.hmget[String, String](mukey(mid), "mu_name", "mu_photo", "twttr").get
            }.get
          case _ => Map.empty[String, String]
        }
      case _ => Map.empty[String, String]
    }
  }

  private def panel(r: RedisClient): Map[String, String] = {
    val Keynote = """boston:panel:(.*)""".r
    r.keys("boston:panel:*") match {
      case None => Map.empty[String, String]
      case Some(skey :: _) =>
        skey match {
          case Some(key) =>
            key match {
              case k @ Keynote(mid) =>
                r.hmget[String, String](k, "name", "desc").map(_ + ("id" -> k)).map {
                  _ ++ r.hmget[String, String](mukey(mid), "mu_name", "mu_photo", "twttr").get
                }.get
              case _ =>
                Map.empty[String, String]
            }
          case _ =>
            Map.empty[String, String]
        }
      case _ => Map.empty[String, String]
    }
  }
}
