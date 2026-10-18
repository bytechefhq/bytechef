version="1.0"

dependencies {
    implementation(libs.org.springaicommunity.typesafe.spring.ai)
    implementation(project(":server:libs:modules:components:ai:agent:guardrails"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    testImplementation("org.mockito:mockito-core")
}
