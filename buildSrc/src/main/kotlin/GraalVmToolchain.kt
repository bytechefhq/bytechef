import org.gradle.api.GradleException
import org.gradle.jvm.toolchain.JavaInstallationMetadata

private val JVMCI_RELEASE_PATTERN = Regex("jvmci-(\\d+)\\.(\\d+)")

fun requiredJvmciRelease(graalVmVersion: String): String = graalVmVersion.split(".").take(2).joinToString(".")

fun verifyGraalVmToolchain(metadata: JavaInstallationMetadata, graalVmVersion: String) {
    val requiredRelease = requiredJvmciRelease(graalVmVersion)
    val jvmVersion = metadata.jvmVersion
    val match = JVMCI_RELEASE_PATTERN.find(jvmVersion)

    val compatible = match != null && compareReleases(
        match.groupValues[1].toInt() to match.groupValues[2].toInt(), requiredRelease) >= 0

    if (!compatible) {
        throw GradleException(
            """
            |GraalVM polyglot $graalVmVersion needs a GraalVM JDK with JVMCI $requiredRelease or newer, but Gradle selected:
            |  ${metadata.installationPath.asFile} (java.vm.version=$jvmVersion)
            |Without a matching JVMCI, Truffle falls back to a runtime that rejects sandbox limits such as
            |sandbox.MaxHeapMemory ("Heap memory limit is not supported on the current Truffle runtime").
            |Install GraalVM CE $graalVmVersion or newer (sdk install java $graalVmVersion-graalce, then sdk env to use .sdkmanrc)
            |and run Gradle with JAVA_HOME pointing at it. Gradle prefers the JDK its daemon runs on, so run
            |./gradlew --stop after switching.
            """.trimMargin())
    }
}

private fun compareReleases(actual: Pair<Int, Int>, required: String): Int {
    val (requiredMajor, requiredMinor) = required.split(".").map { it.toInt() }

    return compareValuesBy(actual, requiredMajor to requiredMinor, { it.first }, { it.second })
}
