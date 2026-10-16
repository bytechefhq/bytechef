dependencies {
    api(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api"))

    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(libs.com.github.docker.java.docker.java.core)
    implementation(libs.com.github.docker.java.docker.java.transport.httpclient5)
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-polyglot"))

    testImplementation("ch.qos.logback:logback-classic")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation(rootProject.libs.org.graalvm.polyglot.js)
    testImplementation(rootProject.libs.org.graalvm.polyglot.python)
    testImplementation(project(":server:libs:test:test-support"))
}
