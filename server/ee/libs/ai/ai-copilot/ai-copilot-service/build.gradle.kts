val copyDocs = tasks.register<Copy>("copyDocs") {
    description = "Copies documentation files to resources"
    group = "build"

    from(rootProject.file("docs/content/docs")) {
        include("**/*.md", "**/*.mdx")
        exclude("reference/components/**", "reference/flow-controls/**")
    }
    into(layout.projectDirectory.dir("src/main/resources/docs"))
}

val copyComponentDocs = tasks.register<Copy>("copyComponentDocs") {
    description = "Copies component README files to resources"
    group = "build"

    from(rootProject.file("server/libs/modules/components")) {
        include("*/src/main/resources/README.md", "*/src/main/resources/README.mdx")
        eachFile {
            val componentName = relativePath.segments[0]
            relativePath = RelativePath(true, "$componentName.mdx")
        }
        includeEmptyDirs = false
    }
    into(layout.projectDirectory.dir("src/main/resources/docs/reference/components"))
}

val copyEeComponentDocs = tasks.register<Copy>("copyEeComponentDocs") {
    description = "Copies EE component README files to resources"
    group = "build"

    from(rootProject.file("server/ee/libs/modules/components")) {
        include("*/src/main/resources/README.md", "*/src/main/resources/README.mdx")
        eachFile {
            val componentName = relativePath.segments[0]
            relativePath = RelativePath(true, "$componentName.mdx")
        }
        includeEmptyDirs = false
    }
    into(layout.projectDirectory.dir("src/main/resources/docs/reference/components"))
}

val copyTaskDispatcherDocs = tasks.register<Copy>("copyTaskDispatcherDocs") {
    description = "Copies task dispatcher README files to resources"
    group = "build"

    from(rootProject.file("server/libs/modules/task-dispatchers")) {
        include("*/src/main/resources/README.md", "*/src/main/resources/README.mdx")
        eachFile {
            val dispatcherName = relativePath.segments[0]
            relativePath = RelativePath(true, "$dispatcherName.mdx")
        }
        includeEmptyDirs = false
    }
    into(layout.projectDirectory.dir("src/main/resources/docs/reference/task-dispatchers"))
}

val copyEeTaskDispatcherDocs = tasks.register<Copy>("copyEeTaskDispatcherDocs") {
    description = "Copies EE task dispatcher README files to resources"
    group = "build"

    from(rootProject.file("server/ee/libs/modules/task-dispatchers")) {
        include("*/src/main/resources/README.md", "*/src/main/resources/README.mdx")
        eachFile {
            val dispatcherName = relativePath.segments[0]
            relativePath = RelativePath(true, "$dispatcherName.mdx")
        }
        includeEmptyDirs = false
    }
    into(layout.projectDirectory.dir("src/main/resources/docs/reference/task-dispatchers"))
}

val copyAllDocs = tasks.register("copyAllDocs") {
    description = "Copies all documentation files to resources"
    group = "build"

    dependsOn(copyDocs, copyComponentDocs, copyEeComponentDocs, copyTaskDispatcherDocs, copyEeTaskDispatcherDocs)
}

tasks.compileJava {
    finalizedBy(copyAllDocs)
}

tasks.processResources {
    dependsOn(copyAllDocs)
}

dependencies {
    implementation(libs.com.github.mizosoft.methanol)
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.ai:spring-ai-commons")
    implementation("org.springframework.ai:spring-ai-pgvector-store")
    implementation("org.springframework.ai:spring-ai-autoconfigure-vector-store-pgvector")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation("org.springframework.ai:spring-ai-advisors-vector-store")
    implementation("org.springframework.ai:spring-ai-anthropic")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(files("../libs/ag-ui/core-0.0.1.jar"))
    implementation(files("../libs/ag-ui/json-0.0.1.jar"))
    implementation(files("../libs/ag-ui/server-0.0.1.jar"))
    implementation(files("../libs/ag-ui/spring-ai-1.0.1.jar"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))
    implementation(project(":server:libs:ai:mcp:mcp-tool:mcp-tool-automation"))
    implementation(project(":server:libs:ai:mcp:mcp-tool:mcp-tool-platform"))

    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-api"))
}
