

resolvers ++= Seq(
  "less is" at "http://repo.lessis.me",
  "coda" at "http://repo.codahale.com")

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.1")

addSbtPlugin("me.lessis" % "heroic" % "0.1.0-SNAPSHOT")
