dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(libs.org.springaicommunity.spring.ai.session)
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:modules:components:ai:agent:chat-memory:chat-memory-session"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
