dependencies {
    api(rootProject.libs.org.graalvm.polyglot.polyglot)

    implementation("io.micrometer:context-propagation")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(rootProject.libs.org.graalvm.polyglot.js)
    testImplementation(rootProject.libs.org.graalvm.polyglot.python)
}
