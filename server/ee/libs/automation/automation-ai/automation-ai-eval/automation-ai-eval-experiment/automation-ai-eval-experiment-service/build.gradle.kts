dependencies {
    api(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-experiment:automation-ai-eval-experiment-api"))
    api(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-experiment:platform-ai-eval-experiment-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-experiment:platform-ai-eval-experiment-service"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-eval:platform-ai-eval-dataset:platform-ai-eval-dataset-service"))
    api(project(":server:ee:libs:automation:automation-ai:automation-ai-eval:automation-ai-eval-dataset:automation-ai-eval-dataset-service"))

    implementation("io.micrometer:micrometer-core")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.retry:spring-retry")
    implementation("org.springframework:spring-web")
    implementation("org.springframework:spring-webflux")
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    // Pulls the observability changesets onto the test classpath so platform/ai/eval's FKs to
    // ai_observability_trace + ai_observability_span resolve when the shared master.xml runs against the test
    // schema. Without these jars the includeAll for platform/ai/observability silently expands to nothing
    // (errorIfMissingOrEmpty="false"), the observability tables never get created, and the eval init fails on
    // its FK add.
    testImplementation(project(":server:ee:libs:automation:automation-ai:automation-ai-observability:automation-ai-observability-service"))
    testImplementation(project(":server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-service"))
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:encryption:encryption-impl"))
    testImplementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    testImplementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    testImplementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))
    testImplementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
