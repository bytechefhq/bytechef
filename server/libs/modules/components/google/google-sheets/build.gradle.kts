version="1.0"

dependencies {
    implementation("com.google.apis:google-api-services-drive:v3-rev20250910-2.0.0")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20250616-2.0.0")
    api(project(":server:libs:modules:components:google:google-commons"))
}
