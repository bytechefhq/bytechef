dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":spring-ai:spring-ag-ui:packages:core"))
    implementation(project(":spring-ai:spring-ag-ui:packages:server"))
    implementation(project(":spring-ai:spring-ag-ui:servers:spring"))
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-api"))
    implementation(project(":server:ee:libs:ai:ai-hub:ai-hub-service"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
