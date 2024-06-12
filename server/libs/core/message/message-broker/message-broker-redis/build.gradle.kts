dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.data:spring-data-redis")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.lettuce:lettuce-core")
    implementation(files("libs/jrsmq-2.0.0.jar"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
}
