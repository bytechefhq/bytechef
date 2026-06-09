plugins {
    id("com.bytechef.java-library-conventions")
}

dependencies {
    api(project(":spring-ai:spring-ag-ui:packages:core"))
    api(project(":spring-ai:spring-ag-ui:packages:server"))

    implementation(project(":spring-ai:spring-ag-ui:utils:json"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-webmvc")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
}
