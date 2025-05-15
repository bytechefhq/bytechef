dependencies {
    implementation("org.eclipse.angus:angus-mail")
    api(project(":server:libs:core:commons:commons-util"))

    testImplementation("com.icegreen:greenmail:2.1.0")
    testImplementation("com.icegreen:greenmail-junit5:2.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":server:libs:config:jackson-config"))

}
