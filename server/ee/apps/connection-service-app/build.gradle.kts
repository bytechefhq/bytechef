group = "com.bytechef.connection"
description = ""

springBoot {
    mainClass.set("com.bytechef.connection.ConnectionApplication")
}

dependencies {
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation(libs.org.springdoc.springdoc.openapi.starter.webmvc.ui)
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.retry:spring-retry")
    implementation(libs.org.springframework.cloud.spring.cloud.starter.loadbalancer)
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))
    implementation(project(":server:libs:configs:liquibase-config"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:encryption:encryption-filesystem"))
    implementation(project(":server:libs:core:rest:rest-impl"))
    implementation(project(":server:libs:core:tag:tag-service"))
    implementation(project(":server:libs:helios:helios-connection:helios-connection-rest"))
    implementation(project(":server:libs:helios:helios-connection:helios-connection-service"))
    implementation(project(":server:libs:hermes:hermes-connection:hermes-connection-service"))

    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
    implementation(project(":server:ee:libs:helios:helios-configuration:helios-configuration-remote-client"))
    implementation(project(":server:ee:libs:hermes:hermes-configuration:hermes-configuration-remote-client"))
    implementation(project(":server:ee:libs:hermes:hermes-connection:hermes-connection-remote-rest"))
    implementation(project(":server:ee:libs:hermes:hermes-definition-registry:hermes-definition-registry-remote-client"))

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.zaxxer:HikariCP")

    testImplementation(project(":server:libs:test:test-int-support"))
}
