val mcpAppWidgets =
    mapOf(
        "workflow-editor" to "WorkflowEditor",
        "data-table-viewer" to "DataTableViewer",
        "code-workflow-viewer" to "CodeWorkflowViewer",
        "custom-component-viewer" to "CustomComponentViewer",
        "file-viewer" to "FileViewer",
    )

val buildMcpApps = providers.gradleProperty("buildMcpApps")
    .map { it.isEmpty() || it.toBoolean() }
    .getOrElse(false)

mcpAppWidgets.forEach { (dirName, taskSuffix) ->
    val widgetDirectory = rootProject.layout.projectDirectory.dir("mcp-apps/$dirName")

    val npmCiTask = tasks.register<Exec>("npmCi$taskSuffix") {
        description = "Installs npm dependencies for the MCP App $dirName widget (requires Node.js)."
        group = "build"

        workingDir = widgetDirectory.asFile

        commandLine("npm", "ci")
    }

    val buildTask = tasks.register<Exec>("build$taskSuffix") {
        description = "Builds the MCP App $dirName widget (npm ci + npm run build; requires Node.js)."
        group = "build"

        dependsOn(npmCiTask)

        workingDir = widgetDirectory.asFile

        commandLine("npm", "run", "build")
    }

    tasks.processResources {
        if (buildMcpApps) {
            dependsOn(buildTask)
        }

        from(widgetDirectory.file("dist/index.html")) {
            into("mcp-apps")
            rename { "$dirName.html" }
        }
    }
}

tasks.register("buildMcpApps") {
    description = "Builds all MCP App viewer widgets (npm ci + npm run build for each; requires Node.js)."
    group = "build"

    dependsOn(mcpAppWidgets.values.map { "build$it" })
}

dependencies {
    implementation("io.modelcontextprotocol.sdk:mcp:${libs.versions.io.modelcontextprotocol.sdk.get()}")
    implementation("org.springframework.ai:mcp-spring-webmvc")
    implementation("org.slf4j:slf4j-api")
    implementation("io.projectreactor:reactor-core")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
