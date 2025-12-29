group = "com.bytechef.connection"
description = ""

springBoot {
    mainClass.set("com.bytechef.connection.ConnectionApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation(libs.org.springdoc.springdoc.openapi.starter.common)
    implementation(libs.org.springdoc.springdoc.openapi.starter.webmvc.ui)
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation(project(":server:libs:automation:automation-swagger"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:async-config"))
    implementation(project(":server:libs:config:environment-config"))
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:config:jdbc-config"))
    implementation(project(":server:libs:config:liquibase-config"))
    implementation(project(":server:libs:config:logback-config"))
    implementation(project(":server:libs:config:messages-config"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:encryption:encryption-filesystem"))
    implementation(project(":server:libs:core:encryption:encryption-impl"))
    implementation(project(":server:libs:core:encryption:encryption-property"))
    implementation(project(":server:libs:core:rest:rest-impl"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-service"))
    implementation(project(":server:libs:platform:platform-oauth2:platform-oauth2-service"))
    implementation(project(":server:libs:platform:platform-swagger"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-service"))

    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-remote-client"))
    implementation(project(":server:ee:libs:config:observability-config"))
    implementation(project(":server:ee:libs:config:tenant-multi-data-config"))
    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
    implementation(project(":server:ee:libs:core:remote:remote-rest"))
    implementation(project(":server:ee:libs:platform:platform-component:platform-component-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-connection:platform-connection-remote-rest"))

    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-client"))
    implementation(project(":server:ee:libs:embedded:embedded-swagger"))

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.zaxxer:HikariCP")

    testImplementation(project(":server:libs:test:test-int-support"))
}
