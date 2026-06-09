plugins {
    id("com.bytechef.java-library-conventions")
}

val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    api(project(":spring-ai:spring-ag-ui:packages:core"))
    api(project(":spring-ai:spring-ag-ui:packages:server"))

    implementation(platform("org.springframework.ai:spring-ai-bom:${libs.findVersion("spring-ai").get()}"))
    implementation(project(":spring-ai:spring-ag-ui:utils:json"))
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("tools.jackson.core:jackson-databind")
}
