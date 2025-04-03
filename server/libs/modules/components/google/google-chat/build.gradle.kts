version="1.0"

dependencies {
    implementation ("com.google.api-client:google-api-client:2.2.0")
    implementation ("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation ("com.google.apis:google-api-services-people:v1-rev20220531-2.0.0")
    api(project(":server:libs:modules:components:google:google-commons"))
}
