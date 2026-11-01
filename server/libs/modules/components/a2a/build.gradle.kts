version = "1.0"

dependencies {
    implementation(libs.io.github.a2asdk.a2a.java.sdk.client)
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
