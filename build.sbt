import scala.slick.codegen.SourceCodeGenerator
import scala.slick.{ model => m }

lazy val databaseUrl = sys.env.getOrElse("DB_DEFAULT_URL", "DB_DEFAULT_URL is not set")

lazy val databaseUser = sys.env.getOrElse("DB_DEFAULT_USER", "DB_DEFAULT_USER is not set")

lazy val databasePassword = sys.env.getOrElse("DB_DEFAULT_PASSWORD", "DB_DEFAULT_PASSWORD is not set")

lazy val flyway = (project in file("flyway"))
  .settings(flywaySettings:_*)
  .settings(
  scalaVersion := "2.11.6",
  flywayUrl := databaseUrl,
  flywayUser := databaseUser,
  flywayPassword := databasePassword,
  flywayLocations := Seq("filesystem:web/conf/db/migration/default")
)

lazy val web = (project in file("web"))
  .enablePlugins(PlayScala)
  .settings(slickCodegenSettings:_*)
  .settings(
    scalaVersion := "2.11.6",
    libraryDependencies ++= Seq(
      jdbc,
      "com.typesafe.play" %% "play-slick" % "0.8.0",
      "com.typesafe.slick" %% "slick" % "2.1.0",
      "com.github.tototoshi" %% "slick-joda-mapper" % "1.2.0",
      "com.h2database" % "h2" % "1.4.186"
    ),
    slickCodegenDatabaseUrl := databaseUrl,
    slickCodegenDatabaseUser := databaseUser,
    slickCodegenDatabasePassword := databasePassword,
    slickCodegenDriver := scala.slick.driver.H2Driver,
    slickCodegenJdbcDriver := "org.h2.Driver",
    slickCodegenOutputPackage := "models",
    slickCodegenExcludedTables := Seq("schema_version"),
    slickCodegenCodeGenerator := { (model:  m.Model) =>
      new SourceCodeGenerator(model) {
        override def code =
          "import com.github.tototoshi.slick.H2JodaSupport._\n" + "import org.joda.time.DateTime\n" + super.code
        override def Table = new Table(_) {
          override def Column = new Column(_) {
            override def rawType = model.tpe match {
              case "java.sql.Timestamp" => "DateTime" // kill j.s.Timestamp
              case _ =>
                super.rawType
            }
          }
        }
      }
    },
    sourceGenerators in Compile <+= slickCodegen
)
