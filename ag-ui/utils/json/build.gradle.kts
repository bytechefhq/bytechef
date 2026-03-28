plugins {
    id("com.bytechef.java-library-conventions")
}

dependencies {
    api(project(":ag-ui:packages:core"))

    implementation("tools.jackson.core:jackson-databind")
}
