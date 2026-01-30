subprojects {
    apply(plugin = "com.bytechef.java-library-conventions")
}

tasks.register<Copy>("copyApiJarsToCliLibs") {
    dependsOn(
        project(":sdks:backend:java:component-api").tasks.named("jar"),
        project(":sdks:backend:java:definition-api").tasks.named("jar")
    )

    from(project(":sdks:backend:java:component-api").tasks.named("jar"))
    from(project(":sdks:backend:java:definition-api").tasks.named("jar"))

    into(rootProject.layout.projectDirectory.dir("cli/cli-app/libs"))
}

tasks.register<Copy>("copyApiJarsToServerLibs") {
    dependsOn(
        project(":sdks:backend:java:component-api").tasks.named("jar"),
        project(":sdks:backend:java:definition-api").tasks.named("jar")
    )

    from(project(":sdks:backend:java:component-api").tasks.named("jar"))
    from(project(":sdks:backend:java:definition-api").tasks.named("jar"))

    into(
        rootProject.layout.projectDirectory.dir(
            "server/ee/libs/platform/platform-api-connector/platform-api-connector-configuration/" +
                "platform-api-connector-configuration-service/src/main/resources/libs"
        )
    )
}

tasks.register("copyApiJars") {
    dependsOn(
        tasks.named("copyApiJarsToCliLibs"),
        tasks.named("copyApiJarsToServerLibs")
    )
}
