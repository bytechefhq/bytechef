// Bundles the MCP App widget (mcp-apps/workflow-editor) into this module's resources as mcp-apps/workflow-editor.html
// (see McpAppWorkflowEditor). Requires Node.js, so it is NOT part of the regular build: run the buildWorkflowEditor
// task (npm run build) first; processResources picks the artifact up only when it exists.
val workflowEditorDirectory = rootProject.layout.projectDirectory.dir("mcp-apps/workflow-editor")

tasks.register<Exec>("buildWorkflowEditor") {
    description =
        "Builds the MCP App workflow editor widget (requires Node.js and npm install in mcp-apps/workflow-editor)."
    group = "build"

    workingDir = workflowEditorDirectory.asFile

    commandLine("npm", "run", "build")
}

tasks.processResources {
    from(workflowEditorDirectory.file("dist/index.html")) {
        into("mcp-apps")
        rename { "workflow-editor.html" }
    }
}

dependencies {
    implementation("io.modelcontextprotocol.sdk:mcp:${libs.versions.io.modelcontextprotocol.sdk.get()}")
    implementation("org.springframework.ai:mcp-spring-webmvc")
    implementation("org.slf4j:slf4j-api")
    implementation("io.projectreactor:reactor-core")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
