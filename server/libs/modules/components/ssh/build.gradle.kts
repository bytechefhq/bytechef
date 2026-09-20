version="1.0"

dependencies {
    implementation("com.hierynomus:sshj:0.40.0")

    testCompileOnly("com.github.spotbugs:spotbugs-annotations")

    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.testcontainers:junit-jupiter")
}
