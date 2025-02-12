subprojects {
    apply(plugin = "com.bytechef.java-library-conventions")

    tasks.jar {
        archiveBaseName.set("ee-" + project.name)
    }
}
