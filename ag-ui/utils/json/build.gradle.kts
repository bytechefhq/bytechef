plugins {
    id("com.bytechef.java-library-conventions")
}

dependencies {
    api(project(":ag-ui:packages:core"))

    implementation("com.fasterxml.jackson.core:jackson-databind")
}
