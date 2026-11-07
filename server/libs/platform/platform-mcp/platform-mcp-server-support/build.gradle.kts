data class McpAppWidget(
    val buildArguments: List<String>,
    val outputFile: String,
    val sourceDirectory: String,
)

val mcpAppWidgets =
    mapOf(
        "workflow-editor" to McpAppWidget(
            listOf("run", "build:mcp-workflow-editor"),
            "dist-mcp-apps/workflow-editor/mcp-workflow-editor.html",
            "client",
        ),
        "data-table-viewer" to McpAppWidget(
            listOf("run", "build"), "dist/index.html", "mcp-apps/data-table-viewer"),
        "code-workflow-viewer" to McpAppWidget(
            listOf("run", "build"), "dist/index.html", "mcp-apps/code-workflow-viewer"),
        "custom-component-viewer" to McpAppWidget(
            listOf("run", "build"), "dist/index.html", "mcp-apps/custom-component-viewer"),
        "file-viewer" to McpAppWidget(listOf("run", "build"), "dist/index.html", "mcp-apps/file-viewer"),
    )

val buildMcpApps = providers.gradleProperty("buildMcpApps")
    .map { it.isEmpty() || it.toBoolean() }
    .getOrElse(false)

fun toTaskSuffix(dirName: String): String =
    dirName.split("-").joinToString("") { word -> word.replaceFirstChar { it.uppercase() } }

val npmCiTasks =
    mcpAppWidgets.values
        .map { it.sourceDirectory }
        .distinct()
        .associateWith { sourceDirectory ->
            tasks.register<Exec>("npmCi${toTaskSuffix(sourceDirectory.substringAfterLast("/"))}") {
                description = "Installs npm dependencies in $sourceDirectory for MCP App widgets (requires Node.js)."
                group = "build"

                workingDir = rootProject.layout.projectDirectory.dir(sourceDirectory).asFile

                commandLine("npm", "ci")
            }
        }

mcpAppWidgets.forEach { (dirName, widget) ->
    val widgetDirectory = rootProject.layout.projectDirectory.dir(widget.sourceDirectory)

    val buildTask = tasks.register<Exec>("build${toTaskSuffix(dirName)}") {
        description = "Builds the MCP App $dirName widget (npm ci + npm ${widget.buildArguments.joinToString(" ")}; requires Node.js)."
        group = "build"

        dependsOn(npmCiTasks.getValue(widget.sourceDirectory))

        workingDir = widgetDirectory.asFile

        commandLine(listOf("npm") + widget.buildArguments)
    }

    tasks.processResources {
        if (buildMcpApps) {
            dependsOn(buildTask)
        }

        from(widgetDirectory.file(widget.outputFile)) {
            into("mcp-apps")
            rename { "$dirName.html" }
        }
    }
}

tasks.register("buildMcpApps") {
    description = "Builds all MCP App viewer widgets (npm ci + npm build for each; requires Node.js)."
    group = "build"

    dependsOn(mcpAppWidgets.keys.map { "build${toTaskSuffix(it)}" })
}

dependencies {
    implementation("io.modelcontextprotocol.sdk:mcp:${libs.versions.io.modelcontextprotocol.sdk.get()}")
    implementation("org.springframework.ai:mcp-spring-webmvc")
    implementation("org.slf4j:slf4j-api")
    implementation("io.projectreactor:reactor-core")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
