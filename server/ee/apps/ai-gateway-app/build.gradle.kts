group = "com.bytechef.AiGatewayApplication"
description = ""

springBoot {
    mainClass.set("com.bytechef.aigateway.AiGatewayApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:async-config"))
    implementation(project(":server:libs:config:cache-config"))
    implementation(project(":server:libs:config:environment-config"))
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:config:logback-config"))
    implementation(project(":server:libs:config:messages-config"))
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-public-rest"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-remote-client"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-service"))
    implementation(project(":server:ee:libs:config:observability-config"))
    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))

    testImplementation(project(":server:libs:test:test-int-support"))
}
