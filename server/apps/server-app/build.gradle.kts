group = "com.bytechef.server"
description = "ByteChef server app"
version = properties["bytechefVersion"].toString()

springBoot {
    mainClass.set("com.bytechef.server.ServerApplication")
}

// =============================================================================
// FAST STARTUP CONFIGURATION FOR INTELLIJ
// =============================================================================
// When fastStartup=true, ALL bytechef library + component modules are presented to IntelliJ as
// pre-built JARs (via the fastStartupRuntime configuration below) instead of exploded module output
// directories. This significantly speeds up IntelliJ startup because ServiceLoader reads from JAR
// manifests (fast) instead of scanning hundreds of directories (slow), and it keeps every module in a
// single base classloader so Spring DevTools does not split them across its base/restart classloaders
// (which otherwise breaks cross-module ServiceLoader/bean wiring).
//
// Usage:
//   1. Set fastStartup=true in gradle.properties (or pass -PfastStartup=true)
//   2. Build JARs first: ./gradlew -PfastStartup=true :server:apps:server-app:buildModuleJars --parallel
//   3. Refresh Gradle in IntelliJ, then run the application
//
// To switch back to project dependencies (for active library/component development with DevTools
// hot-reload): set fastStartup=false in gradle.properties or remove the property.
//
// `useComponentJars` is kept as a deprecated alias for `fastStartup`.
// =============================================================================

val fastStartup = (project.findProperty("fastStartup") ?: project.findProperty("useComponentJars"))
    ?.toString()
    ?.toBoolean() ?: false

// =============================================================================
// COMPONENT FILTERING - Include/exclude specific components at build time
// =============================================================================
// includeComponents: Comma-separated whitelist. If set, ONLY these are loaded.
// excludeComponents: Comma-separated blacklist. If set, these are skipped.
//
// Priority: includeComponents > excludeComponents > load all (default)
//
// | includeComponents | excludeComponents | Result                    |
// |-------------------|-------------------|---------------------------|
// | empty/not set     | empty/not set     | Load ALL components       |
// | comp1,comp2       | (ignored)         | Load ONLY comp1, comp2    |
// | empty/not set     | comp1,comp2       | Load all EXCEPT comp1,comp2|
//
// Component names = directory name under server/libs/modules/components/
// =============================================================================

val includeComponents = project.findProperty("includeComponents")?.toString()
    ?.split(",")
    ?.map { it.trim().lowercase() }
    ?.filter { it.isNotEmpty() }
    ?.toSet()
    ?: emptySet()

val excludeComponents = project.findProperty("excludeComponents")?.toString()
    ?.split(",")
    ?.map { it.trim().lowercase() }
    ?.filter { it.isNotEmpty() }
    ?.toSet()
    ?: emptySet()

fun shouldIncludeComponent(componentPath: String): Boolean {
    val componentName = componentPath.substringAfterLast(":").lowercase()

    // Always exclude the example component
    if (componentName == "example") return false

    // If includeComponents is specified, only include those
    if (includeComponents.isNotEmpty()) {
        return includeComponents.contains(componentName)
    }

    // If excludeComponents is specified, exclude those
    if (excludeComponents.isNotEmpty()) {
        return !excludeComponents.contains(componentName)
    }

    // Default: include all
    return true
}

dependencies {
    developmentOnly(libs.com.julien.dubois.bootui.bootui.spring.boot.starter)
    developmentOnly(libs.it.fabioformosa.quartz.manager.quartz.manager.starter.api)
    developmentOnly(libs.it.fabioformosa.quartz.manager.quartz.manager.starter.ui)

    implementation("io.awspring.cloud:spring-cloud-aws-starter-s3")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    implementation(libs.org.springdoc.springdoc.openapi.starter.common)
    implementation(libs.org.springdoc.springdoc.openapi.starter.webmvc.scalar)
    implementation("org.springframework.ai:spring-ai-autoconfigure-mcp-client-common")
    implementation("org.springframework.ai:spring-ai-starter-mcp-client")
    implementation("org.springframework.ai:spring-ai-starter-model-anthropic")
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory-repository-jdbc")
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory-repository-redis")
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.ai:spring-ai-starter-vector-store-pgvector")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-batch")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-jackson")
    implementation("org.springframework.boot:spring-boot-starter-jms")
    implementation("org.springframework.boot:spring-boot-starter-kafka")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-graphql")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.boot:spring-boot-starter-restclient")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:ai:ai-mcp:ai-mcp-server"))
    implementation(project(":server:libs:ai:ai-mcp:ai-mcp-server-configuration:ai-mcp-server-configuration-graphql"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-config"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-converter"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-git"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-jdbc"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-service"))
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-config"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-jdbc"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-config"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-service"))
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-impl"))
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-config"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-a2a-server"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-a2a:automation-ai-a2a-graphql"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-graphql"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-service"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-mcp-server"))
    implementation(project(":server:libs:automation:automation-asset-file:automation-asset-file-graphql"))
    implementation(project(":server:libs:automation:automation-asset-file:automation-asset-file-rest"))
    implementation(project(":server:libs:automation:automation-asset-file:automation-asset-file-service"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-graphql"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-instance-impl"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-rest:automation-configuration-rest-impl"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    implementation(project(":server:libs:automation:automation-connection:automation-connection-service"))
    implementation(project(":server:libs:automation:automation-data-table:automation-data-table-graphql"))
    implementation(project(":server:libs:automation:automation-data-table:automation-data-table-service"))
    implementation(project(":server:libs:platform:platform-data-table:platform-data-table-service"))
    implementation(project(":server:libs:automation:automation-knowledge-base:automation-knowledge-base-graphql"))
    implementation(project(":server:libs:automation:automation-knowledge-base:automation-knowledge-base-service"))
    implementation(project(":server:libs:platform:platform-knowledge-base:platform-knowledge-base-file-storage:platform-knowledge-base-file-storage-impl"))
    implementation(project(":server:libs:platform:platform-knowledge-base:platform-knowledge-base-rest"))
    implementation(project(":server:libs:platform:platform-knowledge-base:platform-knowledge-base-service"))
    implementation(project(":server:libs:platform:platform-knowledge-base:platform-knowledge-base-worker"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-graphql"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-mcp:automation-ai-mcp-service"))
    implementation(project(":server:libs:automation:automation-openapi"))
    implementation(project(":server:libs:automation:automation-search:automation-search-graphql"))
    implementation(project(":server:libs:automation:automation-search:automation-search-service"))
    implementation(project(":server:libs:automation:automation-task:automation-task-graphql"))
    implementation(project(":server:libs:automation:automation-task:automation-task-service"))
    implementation(project(":server:libs:automation:automation-workflow:automation-workflow-coordinator"))
    implementation(project(":server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-rest"))
    implementation(project(":server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-service"))
    implementation(project(":server:libs:config:ai-chat-memory-config:ai-chat-memory-aws-config"))
    implementation(project(":server:libs:config:ai-chat-memory-config:ai-chat-memory-in-memory-config"))
    implementation(project(":server:libs:config:ai-chat-memory-config:ai-chat-memory-jdbc-config"))
    implementation(project(":server:libs:config:ai-chat-memory-config:ai-chat-memory-redis-config"))
    implementation(project(":server:libs:config:ai-model-config"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:async-config"))
    implementation(project(":server:libs:config:automation-demo-config"))
    implementation(project(":server:libs:config:cache-config"))
    implementation(project(":server:libs:config:environment-config"))
    implementation(project(":server:libs:config:eval-config"))
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:config:jdbc-config"))
    implementation(project(":server:libs:config:liquibase-config"))
    implementation(project(":server:libs:config:logback-config"))
    implementation(project(":server:libs:config:messages-config"))
    implementation(project(":server:libs:config:pgvector-config"))
    implementation(project(":server:libs:config:security-config"))
    implementation(project(":server:libs:config:static-resources-config"))
    implementation(project(":server:libs:config:tenant-single-security-config"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:encryption:encryption-filesystem"))
    implementation(project(":server:libs:core:evaluator:evaluator-impl"))
    implementation(project(":server:libs:core:encryption:encryption-impl"))
    implementation(project(":server:libs:core:encryption:encryption-property"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:core:graphql:graphql-impl"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-jms"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:message:message-event:message-event-impl"))
    implementation(project(":server:libs:core:rest:rest-impl"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:core:tenant:tenant-single-service"))
    implementation(project(":server:libs:licence:licence-service"))
    implementation(project(":server:libs:platform:platform-billing:platform-billing-rest"))
    implementation(project(":server:libs:platform:platform-billing:platform-billing-service"))
    implementation(project(":server:libs:platform:platform-category:platform-category-service"))
    implementation(project(":server:libs:platform:platform-component:platform-component-context:platform-component-context-service"))
    implementation(project(":server:libs:platform:platform-component:platform-component-log:platform-component-log-graphql"))
    implementation(project(":server:libs:platform:platform-component:platform-component-log:platform-component-log-service"))
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-ai-provider"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-graphql"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-service"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-graphql"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-rest:platform-configuration-rest-impl"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-service"))
    implementation(project(":server:libs:platform:platform-coordinator"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-file-storage:platform-data-storage-file-storage-service"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-service"))
    implementation(project(":server:libs:platform:platform-file-storage:platform-file-storage-impl"))
    implementation(project(":server:libs:platform:platform-mcp:platform-mcp-graphql"))
    implementation(project(":server:libs:platform:platform-mcp:platform-mcp-service"))
    implementation(project(":server:libs:platform:platform-notification:platform-notification-rest"))
    implementation(project(":server:libs:platform:platform-notification:platform-notification-service"))
    implementation(project(":server:ee:libs:platform:platform-notification:platform-notification-workspace"))
    implementation(project(":server:libs:platform:platform-oauth2:platform-oauth2-service"))
    implementation(project(":server:libs:platform:platform-oauth2-authorization-server"))
    implementation(project(":server:libs:platform:platform-openapi"))
    implementation(project(":server:libs:platform:platform-plan:platform-plan-service"))
    implementation(project(":server:libs:platform:platform-rate-limit"))
    implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-impl"))
    implementation(project(":server:libs:platform:platform-security:platform-security-graphql"))
    implementation(project(":server:libs:platform:platform-security:platform-security-service"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    implementation(project(":server:libs:platform:platform-user:platform-user-rest"))
    implementation(project(":server:libs:platform:platform-user:platform-user-service"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-impl"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl"))
    implementation(project(":server:libs:platform:platform-webhook:platform-websocket-webhook-rest"))
    implementation(project(":server:libs:platform:platform-worker"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-coordinator:platform-workflow-coordinator-impl"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-service"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-rest"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-test:platform-workflow-test-service"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-rest:platform-workflow-execution-rest-impl"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-service"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-validator:platform-workflow-validator-graphql"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-impl"))

    // CE Components - filtered by includeComponents/excludeComponents properties.
    // When fastStartup=true these project deps are relocated to pre-built JARs below (see
    // fastStartupRuntime); declared as plain project deps here so both modes share one code path.
    rootProject.subprojects
        .asSequence()
        .filter { it.path.startsWith(":server:libs:modules:components") }
        .filter { shouldIncludeComponent(it.path) }
        .sortedBy { it.path }
        .forEach { implementation(project(it.path)) }

    implementation(project(":server:libs:modules:task-dispatchers:approval"))
    implementation(project(":server:libs:modules:task-dispatchers:branch"))
    implementation(project(":server:libs:modules:task-dispatchers:condition"))
    implementation(project(":server:libs:modules:task-dispatchers:each"))
    implementation(project(":server:libs:modules:task-dispatchers:fork-join"))
    implementation(project(":server:libs:modules:task-dispatchers:graph"))
    implementation(project(":server:libs:modules:task-dispatchers:loop"))
    implementation(project(":server:libs:modules:task-dispatchers:map"))
    implementation(project(":server:libs:modules:task-dispatchers:on-error"))
    implementation(project(":server:libs:modules:task-dispatchers:parallel"))
    implementation(project(":server:libs:modules:task-dispatchers:subflow"))
    implementation(project(":server:libs:modules:task-dispatchers:suspend"))
    implementation(project(":server:libs:modules:task-dispatchers:terminate"))

    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-graphql"))
    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-rest"))
    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-service"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-service"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-copilot"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-graphql"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-file-storage"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-jdbc"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-service"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-api"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-graphql"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-rest"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-service"))
    implementation(project(":server:ee:libs:automation:automation-context-store:automation-context-store-graphql"))
    implementation(project(":server:ee:libs:automation:automation-workflow-alert:automation-workflow-alert-graphql"))
    implementation(project(":server:ee:libs:automation:automation-workflow-alert:automation-workflow-alert-service"))
    implementation(project(":server:ee:libs:automation:automation-promotion:automation-promotion-graphql"))
    implementation(project(":server:ee:libs:automation:automation-promotion:automation-promotion-service"))
    implementation(project(":server:ee:libs:automation:automation-workflow-execution-cost:automation-workflow-execution-cost-graphql"))
    implementation(project(":server:ee:libs:automation:automation-workflow-execution-cost:automation-workflow-execution-cost-service"))
    implementation(project(":server:ee:libs:automation:automation-context-store:automation-context-store-service"))
    implementation(project(":server:ee:libs:platform:platform-context-store:platform-context-store-api"))
//    implementation(project(":server:ee:libs:platform:platform-context-store:platform-context-store-clickhouse-service"))
    implementation(project(":server:ee:libs:platform:platform-context-store:platform-context-store-service"))
    implementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-rest"))
    implementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-service"))
    implementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-handler:automation-api-platform-handler-rest"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-graphql"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-service"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-graphql"))
    // Registers the optional OpenNLP-backed SensitiveDataDetector when an operator enables and configures it.
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-opennlp"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-model-catalog:platform-ai-model-catalog-service"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-graphql"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-llm-usage:platform-ai-llm-usage-service"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-service"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-dataset:automation-ai-eval-dataset-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-dataset:automation-ai-eval-dataset-service"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-experiment:automation-ai-eval-experiment-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-experiment:automation-ai-eval-experiment-graphql"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-experiment:automation-ai-eval-experiment-service"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-graphql"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-service"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-service"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-observability:automation-ai-observability-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-observability:automation-ai-observability-service"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-observability:automation-ai-observability-graphql"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway-otlp:platform-ai-gateway-otlp-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway-otlp:platform-ai-gateway-otlp-service"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-prompt:platform-ai-prompt-service"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-graphql"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-service"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-prompt:automation-ai-prompt-graphql"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-prompt:automation-ai-prompt-service"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-graphql"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-public-rest"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-rest"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-service"))
    implementation(project(":server:ee:libs:automation:automation-security-web:automation-security-web-impl"))
    implementation(project(":server:ee:libs:config:cloud-config"))
    implementation(project(":server:ee:libs:config:observability-config"))
    implementation(project(":server:ee:libs:config:tenant-multi-context-store-config"))
    implementation(project(":server:ee:libs:config:tenant-multi-data-config"))
    implementation(project(":server:ee:libs:config:tenant-multi-knowledge-base-config"))
    implementation(project(":server:ee:libs:config:tenant-multi-pgvector-config"))
    implementation(project(":server:ee:libs:config:security-sso-config"))
    implementation(project(":server:ee:libs:config:tenant-multi-security-config"))
    implementation(project(":server:ee:libs:core:cloud:cloud-aws"))
    implementation(project(":server:ee:libs:core:file-storage:file-storage-aws:file-storage-aws-api"))
    implementation(project(":server:ee:libs:core:file-storage:file-storage-aws:file-storage-aws-impl"))
    implementation(project(":server:ee:libs:core:message:message-broker:message-broker-aws"))
    implementation(project(":server:ee:libs:core:tenant:tenant-multi-service"))
    implementation(project(":server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-server"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-graphql"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-rest"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-service"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-graphql"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-instance-impl"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-public-rest"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-rest:embedded-configuration-rest-impl"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-service"))
    implementation(project(":server:ee:libs:embedded:embedded-execution:embedded-execution-public-rest"))
    implementation(project(":server:ee:libs:embedded:embedded-execution:embedded-execution-service"))
    implementation(project(":server:ee:libs:embedded:embedded-ai:embedded-ai-copilot"))
    implementation(project(":server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-graphql"))
    implementation(project(":server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-service"))
    implementation(project(":server:ee:libs:embedded:embedded-openapi"))
    implementation(project(":server:ee:libs:embedded:embedded-security:embedded-security-rest"))
    implementation(project(":server:ee:libs:embedded:embedded-security:embedded-security-service"))
    implementation(project(":server:ee:libs:embedded:embedded-security-web:embedded-security-web-impl"))
    implementation(project(":server:ee:libs:embedded:embedded-unified:embedded-unified-rest"))
    implementation(project(":server:ee:libs:embedded:embedded-unified:embedded-unified-service"))
    implementation(project(":server:ee:libs:embedded:embedded-webhook:embedded-webhook-public-rest"))
    implementation(project(":server:ee:libs:embedded:embedded-workflow:embedded-workflow-coordinator"))
    implementation(project(":server:ee:libs:embedded:embedded-workflow:embedded-workflow-execution:embedded-workflow-execution-rest"))
    implementation(project(":server:ee:libs:embedded:embedded-workflow:embedded-workflow-execution:embedded-workflow-execution-service"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-eval:platform-ai-agent-eval-file-storage:platform-ai-agent-eval-file-storage-impl"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-eval:platform-ai-agent-eval-graphql"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-eval:platform-ai-agent-eval-service"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-service"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-file-storage:platform-ai-skill-file-storage-impl"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-graphql"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-rest"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-service"))
    implementation(project(":server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-graphql"))
    implementation(project(":server:ee:libs:platform:platform-api-connector:platform-api-connector-configuration:platform-api-connector-configuration-service"))
    implementation(project(":server:ee:libs:platform:platform-api-connector:platform-api-connector-file-storage:platform-api-connector-file-storage-impl"))
    implementation(project(":server:ee:libs:platform:platform-api-connector:platform-api-connector-handler"))
    implementation(project(":server:ee:libs:platform:platform-audit:platform-audit-service"))
    implementation(project(":server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-graphql"))
    implementation(project(":server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-service"))
    implementation(project(":server:ee:libs:licence:licence-graphql"))
    implementation(project(":server:ee:libs:licence:licence-service"))
    implementation(project(":server:ee:libs:licence:licence-web"))
    implementation(project(":server:ee:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-service"))
    implementation(project(":server:ee:libs:platform:platform-code-workflow:platform-code-workflow-file-storage:platform-code-workflow-file-storage-impl"))
    implementation(project(":server:ee:libs:platform:platform-component-policy:platform-component-policy-graphql"))
    implementation(project(":server:ee:libs:platform:platform-component-policy:platform-component-policy-service"))
    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-graphql"))
    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-rest"))
    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-service"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-handler"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-rest"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-graphql"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-service"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-file-storage:platform-custom-component-file-storage-impl"))
    implementation(project(":server:ee:libs:platform:platform-resource-grant:platform-resource-grant-service"))
    implementation(project(":server:ee:libs:platform:platform-scheduler:platform-scheduler-aws"))
    implementation(project(":server:ee:libs:platform:platform-scheduler:platform-scheduler-impl"))
    implementation(project(":server:ee:libs:platform:platform-security-web:platform-security-web-impl"))
    implementation(project(":server:ee:libs:platform:platform-user:platform-user-graphql"))
    implementation(project(":server:ee:libs:platform:platform-user:platform-user-scim"))
    implementation(project(":server:ee:libs:platform:platform-user:platform-user-service"))
    implementation(project(":server:ee:libs:platform:platform-variable:platform-variable-graphql"))
    implementation(project(":server:ee:libs:platform:platform-variable:platform-variable-service"))

    // EE Components - filtered by includeComponents/excludeComponents properties.
    // When fastStartup=true these project deps are relocated to pre-built JARs below (see
    // fastStartupRuntime); declared as plain project deps here so both modes share one code path.
    rootProject.subprojects
        .asSequence()
        .filter { it.path.startsWith(":server:ee:libs:modules:components") }
        .filter { shouldIncludeComponent(it.path) }
        .sortedBy { it.path }
        .forEach { implementation(project(it.path)) }

    runtimeOnly("com.h2database:h2")
    runtimeOnly("com.zaxxer:HikariCP")
    runtimeOnly("org.postgresql:postgresql")
    implementation(project(":server:libs:platform:platform-ai:platform-ai-stt:platform-ai-stt-service"))
    runtimeOnly(project(":server:libs:platform:platform-ai:platform-ai-stt:platform-ai-stt-openai"))

    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:core:tenant:tenant-api"))
    testImplementation(project(":server:libs:automation:automation-knowledge-base:automation-knowledge-base-api"))
    testImplementation(project(":server:libs:platform:platform-knowledge-base:platform-knowledge-base-api"))
    testImplementation("org.springframework.boot:spring-boot-jdbc")
    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
}

// =============================================================================
// FAST STARTUP - relocate bytechef project deps to pre-built JARs (see the flag near the top)
// =============================================================================
// Resolvable configuration that forces the runtime-JAR variant and therefore carries each module's FULL
// transitive closure (bytechef JARs + third-party JARs) - unlike a bare files(jarTask), which drops
// transitives and is why the old component-only jar swap broke with classloader errors.
val fastStartupRuntime: Configuration = configurations.create("fastStartupRuntime") {
    isCanBeResolved = true
    isCanBeConsumed = false
    isVisible = false

    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category::class.java, Category.LIBRARY))
        attribute(
            LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE,
            objects.named(LibraryElements::class.java, LibraryElements.JAR))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling::class.java, Bundling.EXTERNAL))
    }

    // Mirror the java-common-conventions exclude so the closure matches the normal runtimeClasspath.
    exclude(mapOf("group" to "org.slf4j", "module" to "slf4j-simple"))
}

// When fastStartup=true, move every bytechef project(...) dependency off implementation/runtimeOnly onto
// fastStartupRuntime, then feed the resolved transitive JAR closure back as a single file dependency.
// IntelliJ then models the bytechef modules as external JAR libraries (single base classloader), which is
// what speeds up startup and avoids the DevTools base/restart classloader split. Third-party deps (declared
// as string coordinates, not project(...)) are left untouched. Normal mode leaves everything as-is.
if (fastStartup) {
    listOf("implementation", "runtimeOnly").forEach { configurationName ->
        val configuration = configurations.getByName(configurationName)

        configuration.dependencies.withType(ProjectDependency::class.java)
            .toList()
            .forEach { projectDependency ->
                fastStartupRuntime.dependencies.add(
                    project.dependencies.project(mapOf("path" to projectDependency.path)))

                configuration.dependencies.remove(projectDependency)
            }
    }

    dependencies {
        implementation(files(fastStartupRuntime))
    }
}

configure<com.gorylenko.GitPropertiesPluginExtension> {
    dotGitDirectory = project.rootProject.layout.projectDirectory.dir(".git")
}

tasks.named<Test>("testIntegration") {
    maxHeapSize = "1g"
}

// Task to build all bytechef library + component JARs for the fast IntelliJ startup. Run with
// -PfastStartup=true so fastStartupRuntime is populated and only the exact transitive JAR closure is built;
// without the flag it falls back to building just the component JARs (respects includeComponents/excludeComponents).
val buildModuleJars by tasks.registering {
    group = "build"
    description = "Build the bytechef module JARs used by fastStartup (respects includeComponents/excludeComponents filters)"

    if (fastStartup) {
        // Building fastStartupRuntime's artifacts builds every module JAR in the resolved transitive closure.
        dependsOn(fastStartupRuntime)
    } else {
        val filteredComponents = rootProject.subprojects
            .filter { it.path.startsWith(":server:libs:modules:components") || it.path.startsWith(":server:ee:libs:modules:components") }
            .filter { shouldIncludeComponent(it.path) }

        dependsOn(filteredComponents.map { "${it.path}:jar" })
    }

    doLast {
        if (fastStartup) {
            println("\n✅ Built the bytechef module JAR closure for fastStartup. Refresh Gradle in IntelliJ, then run server-app.")
        } else {
            println("\n⚠️  fastStartup is off - built only component JARs. Re-run with -PfastStartup=true (and set")
            println("    fastStartup=true in gradle.properties) to build the full module JAR set and speed up IntelliJ startup.")
        }

        if (includeComponents.isNotEmpty()) {
            println("📋 Whitelist (includeComponents): ${includeComponents.joinToString(", ")}")
        }

        if (excludeComponents.isNotEmpty()) {
            println("🚫 Blacklist (excludeComponents): ${excludeComponents.joinToString(", ")}")
        }
    }
}

// Deprecated alias for buildModuleJars (kept for back-compat).
val buildComponentJars by tasks.registering {
    group = "build"
    description = "Deprecated alias for buildModuleJars"

    dependsOn(buildModuleJars)
}

// Generates the build-time component index (META-INF/bytechef/component-index.json) consumed by
// ComponentDefinitionRegistry: the components-list view is served from this index without loading a single
// component handler, and individual components are loaded on demand via the recorded provider class names.
// When the index is absent (e.g. tests, apps that don't run this task), the registry falls back to full loading.
val generateComponentIndex by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Generate the component index consumed by ComponentDefinitionRegistry"

    val outputDir = layout.buildDirectory.dir("generated/component-index")
    val outputFile = outputDir.map { it.file("META-INF/bytechef/component-index.json") }

    // Only the dependency jars — the generator class and every component come from the runtime classpath
    // configuration; using the app's own compiled output here would create a compileJava <-> processResources cycle.
    classpath = files(configurations.runtimeClasspath.get())
    mainClass.set("com.bytechef.platform.component.index.ComponentIndexGenerator")

    argumentProviders.add(CommandLineArgumentProvider { listOf(outputFile.get().asFile.absolutePath) })

    // Component jars influence the index content; platform-component-service carries the generator and the
    // index format itself, so changes to either must re-run the sweep (a stale-format index would silently
    // disable lazy loading until a clean build).
    inputs.files(
        configurations.runtimeClasspath.get()
            .filter { it.path.contains("components") || it.path.contains("platform-component-service") })
    outputs.dir(outputDir)
}

tasks.named<ProcessResources>("processResources") {
    from(generateComponentIndex)
}
