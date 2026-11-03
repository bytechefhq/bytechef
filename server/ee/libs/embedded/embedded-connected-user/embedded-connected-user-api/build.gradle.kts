plugins {
    `java-test-fixtures`
}

configurations.testFixturesImplementation {
    extendsFrom(configurations.implementation.get())
}

dependencies {
    api("org.springframework.data:spring-data-jdbc")
    api(project(":server:libs:platform:platform-connection:platform-connection-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:libs:platform:platform-security-web:platform-security-web-api"))

    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:core:commons:commons-util"))

    testFixturesCompileOnly(rootProject.libs.com.github.spotbugs.spotbugs.annotations)
    testFixturesImplementation("org.assertj:assertj-core")
    testFixturesImplementation("org.mockito:mockito-core")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
