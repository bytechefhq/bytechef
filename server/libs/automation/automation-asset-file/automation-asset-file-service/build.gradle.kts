dependencies {
    api(project(":server:libs:automation:automation-asset-file:automation-asset-file-api"))
    api(project(":server:libs:automation:automation-asset-file:automation-asset-file-file-storage"))
    api(project(":server:libs:automation:automation-search:automation-search-api"))

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:${rootProject.libs.versions.spring.boot.get()}")

    implementation("io.micrometer:micrometer-core")
    implementation("org.apache.tika:tika-core:3.2.3")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    testImplementation(project(":server:libs:platform:platform-category:platform-category-service"))
    testImplementation(project(":server:libs:platform:platform-security:platform-security-service"))
    testImplementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
    testImplementation("org.testcontainers:postgresql")
}

val assetFileCallerScan = tasks.register<Test>("assetFileCallerScan") {
    useJUnitPlatform()

    description = "Runs the cross-module AssetFileSystemFacade caller scan. Never cached, never up to date."
    group = "verification"

    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath

    dependsOn(tasks.testClasses)
    include("**/AssetFileSystemFacadeCallerScanTest*")

    outputs.upToDateWhen { false }
}

tasks.test {
    exclude("**/AssetFileSystemFacadeCallerScanTest*")
}

tasks.check {
    dependsOn(assetFileCallerScan)
}
