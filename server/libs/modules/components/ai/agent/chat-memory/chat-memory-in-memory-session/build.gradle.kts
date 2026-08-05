dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session)
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
