// Bundles the MCP App widgets (mcp-apps/<name>) into this module's resources as mcp-apps/<name>.html (served by
// McpApps). Building the widgets requires Node.js, so it is OFF by default to keep the regular Java build Node-free
// and fast: processResources still picks up a widget's dist/index.html whenever it exists.
//
// Production/release builds must produce those bundles. Do EITHER of:
//   * `./gradlew :...:platform-mcp-server-support:processResources -PbuildMcpApps` (or set buildMcpApps=true in
//     gradle.properties) on a machine with Node.js — this makes processResources first run npm ci + npm run build for
//     every widget, so the bundles are baked into the jar; or
//   * `./gradlew buildMcpApps` (aggregate task) beforehand, then the normal build.
// Without either, the viewers are simply not served (the server still starts) — the same graceful degradation the
// phase-1 workflow-editor already had.
val mcpAppWidgets =
    mapOf(
        "workflow-editor" to "WorkflowEditor",
        "data-table-viewer" to "DataTableViewer",
        "code-workflow-viewer" to "CodeWorkflowViewer",
        "custom-component-viewer" to "CustomComponentViewer",
        "file-viewer" to "FileViewer",
    )

// Enabled by `-PbuildMcpApps` (bare, empty value) or `-PbuildMcpApps=true`; disabled when absent or `=false`.
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
        // Only build the widget as part of processResources when explicitly opted in (release builds); otherwise just
        // bundle a pre-built dist/index.html if one happens to be present.
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
