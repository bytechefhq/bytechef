plugins {
    `java-test-fixtures`
}

configurations.testFixturesImplementation {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    api("org.springframework.security:spring-security-config")
    api("org.springframework.security:spring-security-web")
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:tenant:tenant-api"))

    compileOnly("jakarta.servlet:jakarta.servlet-api")

    testFixturesCompileOnly(rootProject.libs.com.github.spotbugs.spotbugs.annotations)
    testFixturesImplementation("org.assertj:assertj-core")
    testFixturesImplementation("org.mockito:mockito-core")

    testImplementation("jakarta.servlet:jakarta.servlet-api")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework:spring-test")
}
