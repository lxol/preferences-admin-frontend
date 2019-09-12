import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt._

object FrontendBuild extends Build with MicroService {

  val appName = "preferences-admin-frontend"

  override lazy val appDependencies: Seq[ModuleID] = compile ++ test()

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "5.0.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.38.0-play-25",
    "uk.gov.hmrc" %% "play-ui" % "7.40.0-play-25",
    "uk.gov.hmrc" %% "play-partials" % "6.9.0-play-25"
  )

  def test(scope: String = "test") = Seq(
    "uk.gov.hmrc" %% "hmrctest" % "3.9.0-play-25" % scope,
    "org.scalatest" %% "scalatest" % "3.0.1" % scope,
    "org.scalactic" %% "scalactic" % "3.0.1" % scope,
    "org.pegdown" % "pegdown" % "1.6.0" % scope,
    "org.jsoup" % "jsoup" % "1.8.1" % scope,
    "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
    "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
    "org.mockito" % "mockito-core" % "2.7.20" % scope,
    "org.scalamock" %% "scalamock-scalatest-support" % "3.5.0" % scope
  )

}
