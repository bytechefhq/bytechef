version="1.0"

dependencies {
    implementation ("com.google.api-client:google-api-client:2.2.0")
    implementation ("com.google.oauth-client:google-oauth-client-jetty:1.34.1")
    implementation ("com.google.apis:google-api-services-docs:v1-rev20220609-2.0.0")
    implementation ("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    api(project(":server:libs:modules:components:google:google-commons"))
}
