dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(libs.com.github.docker.java.docker.java.core)
    implementation(libs.com.github.docker.java.docker.java.transport.httpclient5)
    implementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation(libs.commons.compress)
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-runner:platform-component-runner-impl"))
    testImplementation(project(":server:libs:test:test-support"))
}
