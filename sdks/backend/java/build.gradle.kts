subprojects {
    apply(plugin = "com.bytechef.java-library-conventions")

    afterEvaluate {
        if (name == "component-api" || name == "definition-api") {
            tasks.named("compileJava") {
                finalizedBy(":sdks:backend:java:copyApiJars")
            }
        }
    }
}

tasks.register("copyApiJars") {
    dependsOn(
        project(":sdks:backend:java:component-api").tasks.named("jar"),
        project(":sdks:backend:java:definition-api").tasks.named("jar")
    )

    doLast {
        val jarFiles = listOf(
            project(":sdks:backend:java:component-api").tasks.named("jar").get().outputs.files,
            project(":sdks:backend:java:definition-api").tasks.named("jar").get().outputs.files
        )

        val destinations = listOf(
            rootProject.file("cli/cli-app/libs"),
            rootProject.file("server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/platform-api-connector-configuration-service/src/main/resources/libs")
        )

        destinations.forEach { destination ->
            destination.mkdirs()

            jarFiles.forEach { files ->
                files.forEach { file ->
                    file.copyTo(destination.resolve(file.name), overwrite = true)
                }
            }
        }
    }
}
