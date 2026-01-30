subprojects {
    apply(plugin = "com.bytechef.java-library-conventions")
}

tasks.register("copyApiJars") {
    val componentApiJar = project(":sdks:backend:java:component-api").tasks.named<Jar>("jar")
    val definitionApiJar = project(":sdks:backend:java:definition-api").tasks.named<Jar>("jar")

    dependsOn(componentApiJar, definitionApiJar)

    inputs.files(componentApiJar, definitionApiJar)

    val cliLibsDir = rootProject.layout.projectDirectory.dir("cli/cli-app/libs")
    val serverLibsDir = rootProject.layout.projectDirectory.dir(
        "server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/" +
            "platform-api-connector-configuration-service/src/main/resources/libs"
    )

    outputs.dir(cliLibsDir)
    outputs.dir(serverLibsDir)

    doLast {
        listOf(cliLibsDir, serverLibsDir).forEach { destinationDir ->
            project.copy {
                from(componentApiJar)
                from(definitionApiJar)
                into(destinationDir)
            }
        }
    }
}
