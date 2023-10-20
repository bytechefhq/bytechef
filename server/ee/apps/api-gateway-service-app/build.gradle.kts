group = "com.bytechef.api"
description = ""

springBoot {
    mainClass.set("com.bytechef.api.gateway.ApiGatewayApplication")
}

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
    implementation(platform(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES))

    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))

    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.springframework.cloud:spring-cloud-starter-loadbalancer")

    testImplementation(project(":server:libs:test:test-int-support"))
}
