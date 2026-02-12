group = "com.bytechef.api"
description = ""

springBoot {
    mainClass.set("com.bytechef.api.gateway.ApiGatewayApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-gateway-server-webmvc")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:config:logback-config"))

    implementation(project(":server:ee:libs:config:observability-config"))
    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))

    testImplementation(project(":server:libs:test:test-int-support"))
}
