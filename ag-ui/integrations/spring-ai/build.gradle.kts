plugins {
    id("com.bytechef.java-library-conventions")
}

val libs = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")

dependencies {
    api(project(":ag-ui:packages:core"))
    api(project(":ag-ui:packages:server"))

    implementation(platform("org.springframework.ai:spring-ai-bom:${libs.findVersion("spring-ai").get()}"))
    implementation(project(":ag-ui:utils:json"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.ai:spring-ai-client-chat")
}
