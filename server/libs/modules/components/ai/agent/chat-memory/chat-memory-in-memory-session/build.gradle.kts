dependencies {
    implementation(libs.org.springaicommunity.spring.ai.session)
    implementation("org.slf4j:slf4j-api")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
