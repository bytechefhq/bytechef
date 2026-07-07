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

spotless {
    json {
        target("src/**/*.json")
        targetExclude("src/main/resources/docs/**")
    }
    yaml {
        target("src/**/*.yaml")
        targetExclude("src/main/resources/docs/**")
    }
}

dependencies {
    implementation(libs.com.github.mizosoft.methanol)
    implementation("com.openai:openai-java-client-okhttp")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.ai:spring-ai-commons")
    implementation("org.springframework.ai:spring-ai-ollama")
    implementation("org.springframework.ai:spring-ai-openai")
    implementation("org.springframework.ai:spring-ai-pgvector-store")
    implementation("org.springframework.ai:spring-ai-autoconfigure-vector-store-pgvector")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation("org.springframework.ai:spring-ai-vector-store-advisor")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-core")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":spring-ai:spring-ag-ui:packages:core"))
    implementation(project(":spring-ai:spring-ag-ui:utils:json"))
    implementation(project(":spring-ai:spring-ag-ui:packages:server"))
    implementation(project(":spring-ai:spring-ag-ui:integrations:spring-ai"))
    implementation(project(":server:libs:ai:ai-mcp:ai-mcp-server-api"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-tool"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-tool"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))

    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-api"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-tool"))

    implementation(project(":server:libs:core:tenant:tenant-api"))
}
