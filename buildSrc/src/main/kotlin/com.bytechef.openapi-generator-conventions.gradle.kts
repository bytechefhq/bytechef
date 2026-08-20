import java.net.URLClassLoader
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

/*
 * Repo-wide wiring for the vendored openapi-generator template in <rootDir>/config/openapi-templates.
 *
 * openapi-generator 7.24.0's "spring" generator started unconditionally emitting
 * @JsonInclude(Include.NON_NULL) (and @JsonInclude(Include.NON_ABSENT) for nullable properties) on every
 * optional model field (SpringCodegen#postProcessModelProperty, unconditional since 7.24.0 - no
 * configOptions/additionalProperties flag gates it). That silently drops explicit "field": null from every
 * response, a wire-format break unrelated to any schema change. There is no generator-level switch to opt
 * out, so config/openapi-templates/pojo.mustache overrides just the affected template with the two
 * @JsonInclude/@JsonSetter(nulls = SKIP) blocks removed, restoring pre-7.24.0 serialization.
 *
 * This plugin exists because that protection used to be wired in exactly ONE of the repo's 23 spring-generator
 * modules, while every other one would silently generate the annotations on its next regeneration. Root's
 * `subprojects { apply(...) }` applies this plugin to every project, so a module is covered by registering a
 * spring GenerateTask - there is nothing to remember and nothing to opt into.
 */

// The template lives in the ROOT project's config/ directory, next to config/checkstyle, config/pmd and
// config/spotbugs - this repo's established home for shared, build-wide tool configuration.
//
// The file sits at the top of that directory, NOT under a "JavaSpring/" subdirectory: openapi-generator
// resolves templateDir entries FLAT (the same layout its own `author template -g spring` extraction
// produces). It consults this directory first and falls back to its embedded templates for anything not
// present here, so every other 7.24.0 template fix still applies.
val openApiTemplateDirectory: Directory = rootProject.layout.projectDirectory.dir("config/openapi-templates")

// GenerateTask cannot be referenced by type here. Putting openapi-generator-gradle-plugin on buildSrc's
// classpath would place it on every project's buildscript classpath, and Gradle then rejects each module's
// `plugins { alias(libs.plugins.org.openapi.generator) }` with "the plugin is already on the classpath with
// an unknown version, so compatibility cannot be checked" - all 37 of them. So the task is identified by
// class name instead, the same untyped shape com.bytechef.java-library-conventions already uses to
// post-process generated clients.
val openApiGenerateTaskClassName = "org.openapitools.generator.gradle.plugin.tasks.GenerateTask"

fun isOpenApiGenerateTask(task: Task): Boolean {
    // Gradle decorates task classes, so the runtime class is GenerateTask_Decorated - walk up to find it.
    var candidateClass: Class<*>? = task.javaClass

    while (candidateClass != null) {
        if (candidateClass.name == openApiGenerateTaskClassName) {
            return true
        }

        candidateClass = candidateClass.superclass
    }

    return false
}

@Suppress("UNCHECKED_CAST")
fun generatorNameOf(task: Task): Property<String> =
    task.javaClass.getMethod("getGeneratorName").invoke(task) as Property<String>

fun templateDirOf(task: Task): DirectoryProperty =
    task.javaClass.getMethod("getTemplateDir").invoke(task) as DirectoryProperty

plugins.withId("org.openapi.generator") {
    tasks.configureEach {
        if (!isOpenApiGenerateTask(this)) {
            return@configureEach
        }

        // A convention rather than a value: it applies whatever the ordering between this action and the
        // module's own registration block, and a module that genuinely needs its own templates still wins by
        // calling templateDir.set(...). The provider is absent for every non-spring generator - the repo's
        // typescript-fetch and java client generators do not read pojo.mustache - so those tasks keep the
        // generator's embedded templates untouched.
        templateDirOf(this).convention(
            generatorNameOf(this)
                .filter { generatorName -> generatorName == "spring" }
                .map { openApiTemplateDirectory })
    }
}

fun registerOpenApiPojoTemplateDriftGuard() {
    // The two blocks config/openapi-templates/pojo.mustache exists to delete, verbatim (leading indentation
    // included) as openapi-generator 7.24.0 writes them in its own JavaSpring/pojo.mustache.
    val jsonIncludeTemplateBlock = listOf(
        "  {{#jackson}}",
        "  {{^required}}",
        "  {{^isNullable}}",
        "  @JsonInclude(JsonInclude.Include.NON_NULL)",
        "  {{/isNullable}}",
        "  {{/required}}",
        "  {{#vendorExtensions.x-is-jackson-optional-nullable}}",
        "  @JsonInclude(JsonInclude.Include.NON_ABSENT)",
        "  {{/vendorExtensions.x-is-jackson-optional-nullable}}",
        "  {{/jackson}}")

    val jsonSetterNullsSkipTemplateBlock = listOf(
        "  {{#jackson}}",
        "  {{#vendorExtensions.x-has-json-setter-nulls-skip}}",
        "  @JsonSetter(nulls = Nulls.SKIP)",
        "  {{/vendorExtensions.x-has-json-setter-nulls-skip}}",
        "  {{/jackson}}")

    val libs = rootProject.extensions.getByType<VersionCatalogsExtension>()
        .named("libs")
    val openApiGeneratorVersion = libs.findPlugin("org-openapi-generator")
        .get()
        .get()
        .version
        .requiredVersion

    // The guard used to read the template off the build classpath, which only worked in a module that applied
    // the generator plugin. The root project deliberately does not (see above), so the generator's own
    // artifact is resolved explicitly instead - same jar, same version, pinned to the same catalog entry the
    // modules generate with.
    // Detached, so it is not registered in the project's configuration container: the version-report and
    // version-catalog-update plugins applied to this same root project enumerate configurations, and this one
    // is a private build-time lookup, not a declared dependency of the repo.
    val openApiGeneratorTemplates = configurations.detachedConfiguration(
        dependencies.create("org.openapitools:openapi-generator:$openApiGeneratorVersion"))

    val vendoredFile = openApiTemplateDirectory.file("pojo.mustache")
        .asFile
    val stampFile = layout.buildDirectory.file("openapi-template-drift/pojo.mustache.verified")
    val generatorClasspath: FileCollection = openApiGeneratorTemplates

    // Guards the vendoring above. Without it, an openapi-generator upgrade leaves the repo silently generating
    // from 7.24.0's template forever - every later fix to JavaSpring/pojo.mustache stops arriving, with nothing
    // to notice it. The task reads the generator's own template out of the resolved openapi-generator artifact
    // (so it always compares against the version actually in use), deletes the two blocks, and requires the
    // result to equal the vendored copy: the vendoring's contract, "upstream minus exactly these two blocks",
    // asserted rather than documented.
    val verifyOpenApiPojoTemplate = tasks.register("verifyOpenApiPojoTemplate") {
        description = "Fails when config/openapi-templates/pojo.mustache has drifted from openapi-generator's own template."
        group = "verification"

        inputs.file(vendoredFile)
        inputs.files(generatorClasspath)
        // A generator bump changes this, so the task re-runs instead of reporting up-to-date against the old jar.
        inputs.property("openApiGeneratorVersion", openApiGeneratorVersion)
        outputs.file(stampFile)

        doLast {
            val howToFix =
                """
                |How to fix, in order:
                |  1. Extract the NEW generator's template: `openapi-generator author template -g spring`, or read
                |     JavaSpring/pojo.mustache straight out of the openapi-generator jar.
                |  2. Delete the @JsonInclude block and the @JsonSetter(nulls = SKIP) block from it - the same two
                |     removals, on the new text. Change nothing else.
                |  3. Overwrite config/openapi-templates/pojo.mustache with the result.
                |  4. Only if the blocks themselves changed shape: update jsonIncludeTemplateBlock /
                |     jsonSetterNullsSkipTemplateBlock in
                |     buildSrc/src/main/kotlin/com.bytechef.openapi-generator-conventions.gradle.kts to match, so
                |     this task keeps guarding.
                |  5. Regenerate (`./gradlew generateOpenAPI`) and confirm the models still carry no @JsonInclude.
                |
                |Do NOT simply copy the generator's template over the vendored one - that reinstates the annotations
                |and silently drops explicit "field": null from every response of every REST API in this repo.
                """.trimMargin()

            val templateResource = URLClassLoader(
                generatorClasspath.files
                    .map { jarFile -> jarFile.toURI().toURL() }
                    .toTypedArray(),
                null)
                .getResource("JavaSpring/pojo.mustache")
                ?: throw GradleException(
                    "openapi-generator $openApiGeneratorVersion no longer ships JavaSpring/pojo.mustache, so the " +
                        "vendored config/openapi-templates/pojo.mustache cannot be checked for drift.\n\n" + howToFix)

            val generatorLines = templateResource.readText()
                .replace("\r\n", "\n")
                .split("\n")
                .toMutableList()

            for ((blockName, blockLines) in listOf(
                "@JsonInclude" to jsonIncludeTemplateBlock,
                "@JsonSetter(nulls = SKIP)" to jsonSetterNullsSkipTemplateBlock)) {

                val blockIndex = generatorLines.windowed(blockLines.size)
                    .indexOf(blockLines)

                if (blockIndex < 0) {
                    throw GradleException(
                        "openapi-generator's JavaSpring/pojo.mustache no longer contains the $blockName block the " +
                            "repo vendors the template to remove. The vendored copy is now overriding a template " +
                            "that has moved on.\n\n" + howToFix)
                }

                repeat(blockLines.size) { generatorLines.removeAt(blockIndex) }
            }

            val expectedLines = generatorLines
            val vendoredLines = vendoredFile.readText()
                .replace("\r\n", "\n")
                .split("\n")

            if (expectedLines != vendoredLines) {
                val firstDifferingLine = expectedLines.zip(vendoredLines)
                    .indexOfFirst { (expected, vendored) -> expected != vendored }
                    .let { if (it < 0) minOf(expectedLines.size, vendoredLines.size) else it }

                throw GradleException(
                    "config/openapi-templates/pojo.mustache is no longer openapi-generator's own " +
                        "JavaSpring/pojo.mustache minus the two known blocks - they first differ at line " +
                        "${firstDifferingLine + 1} (generator's template minus the blocks: ${expectedLines.size} " +
                        "lines, vendored copy: ${vendoredLines.size} lines). Either the generator was upgraded and " +
                        "the vendored copy was not re-derived, or the vendored copy was hand-edited beyond the two " +
                        "removals.\n\n" + howToFix)
            }

            val stamp = stampFile.get()
                .asFile

            stamp.parentFile.mkdirs()
            stamp.writeText("config/openapi-templates/pojo.mustache matches the generator's template minus the two blocks.\n")
        }
    }

    tasks.matching { task -> task.name == "check" }.configureEach {
        dependsOn(verifyOpenApiPojoTemplate)
    }
}

// One template, so one drift guard: it is registered on the root project only, and every spring GenerateTask
// in the repo now generates from the file it checks.
if (project == rootProject) {
    registerOpenApiPojoTemplateDriftGuard()
}
