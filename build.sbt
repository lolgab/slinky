organization in ThisBuild := "me.shadaj"

scalaVersion in ThisBuild := "2.12.4"

scalacOptions in ThisBuild ++= Seq("-feature", "-deprecation")

lazy val slinky = project.in(file(".")).aggregate(
  readWrite,
  core,
  web,
  testRenderer,
  native,
  vr,
  hot,
  scalajsReactInterop
).settings(publishArtifact := false)

addCommandAlias(
  "publishSignedAll",
  (slinky: ProjectDefinition[ProjectReference])
    .aggregate
    .map(p => s"${p.asInstanceOf[LocalProject].project}/publishSigned")
    .mkString(";", ";", "")
)

lazy val macroAnnotationSettings = Seq(
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val generator = project

lazy val readWrite = project

lazy val core = project.settings(macroAnnotationSettings).dependsOn(readWrite)

lazy val web = project.settings(
  sourceGenerators in Compile += Def.taskDyn[Seq[File]] {
    val rootFolder = (sourceManaged in Compile).value / "slinky/web"
    rootFolder.mkdirs()

    val html = (runMain in Compile in generator).toTask(Seq("slinky.generator.Generator", "web/html.json", (rootFolder / "html").getAbsolutePath, "slinky.web.html").mkString(" ", " ", "")).map { _ =>
      (rootFolder / "html" ** "*.scala").get
    }

    val svg = (runMain in Compile in generator).toTask(Seq("slinky.generator.Generator", "web/svg.json", (rootFolder / "svg").getAbsolutePath, "slinky.web.svg").mkString(" ", " ", "")).map { _ =>
      (rootFolder / "svg" ** "*.scala").get
    }

    html.zip(svg).flatMap(t => t._1.flatMap(h => t._2.map(s => h ++ s)))
  }.taskValue,
  mappings in (Compile, packageSrc) ++= {
    val base  = (sourceManaged  in Compile).value
    val files = (managedSources in Compile).value
    files.map { f => (f, f.relativeTo(base).get.getPath) }
  }
).dependsOn(core)

lazy val testRenderer = project.settings(macroAnnotationSettings).dependsOn(core)

lazy val native = project.settings(macroAnnotationSettings).dependsOn(core, testRenderer % Test)

lazy val vr = project.settings(macroAnnotationSettings).dependsOn(core, testRenderer % Test)

lazy val hot = project.settings(macroAnnotationSettings).dependsOn(core)

lazy val scalajsReactInterop = project.settings(macroAnnotationSettings).dependsOn(core)

lazy val tests = project.settings(macroAnnotationSettings).dependsOn(core, web, hot, scalajsReactInterop)

lazy val example = project.settings(macroAnnotationSettings).dependsOn(web, hot, scalajsReactInterop)

lazy val docsMacros = project.settings(macroAnnotationSettings).dependsOn(web, hot, scalajsReactInterop)

lazy val docs = project.settings(macroAnnotationSettings).dependsOn(web, hot, scalajsReactInterop, docsMacros)
