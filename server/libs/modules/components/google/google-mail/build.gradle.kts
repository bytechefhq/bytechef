version="1.0"

dependencies {
    implementation("com.google.api-client:google-api-client:2.2.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation("com.google.apis:google-api-services-gmail:v1-rev20220404-2.0.0")
    implementation("org.eclipse.angus:angus-mail:2.0.2")
    api(project(":server:libs:modules:components:google:google-commons"))

    testImplementation(project(":server:libs:platform:platform-component:platform-component-service"))
    testImplementation(project(":server:libs:test:test-support"))
}
