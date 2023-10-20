import org.springframework.boot.gradle.tasks.bundling.BootJar

val bootJar by tasks.existing(BootJar::class) {
    enabled = false
}
