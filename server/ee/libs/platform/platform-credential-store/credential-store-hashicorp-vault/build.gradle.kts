dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(libs.org.springframework.vault.spring.vault.core)
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-credential-store:platform-credential-store-api"))

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:vault")
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
