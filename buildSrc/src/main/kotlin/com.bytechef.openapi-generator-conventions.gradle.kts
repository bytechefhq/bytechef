import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property

val openApiGenerateTaskClassName = "org.openapitools.generator.gradle.plugin.tasks.GenerateTask"

fun isOpenApiGenerateTask(task: Task): Boolean {
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

@Suppress("UNCHECKED_CAST")
fun additionalPropertiesOf(task: Task): MapProperty<String, Any> =
    task.javaClass.getMethod("getAdditionalProperties").invoke(task) as MapProperty<String, Any>

val springJacksonAnnotationOptions = mapOf<String, Any>(
    "generateJsonIncludeAnnotations" to "false",
    "generateJsonSetterNullsAnnotations" to "false")

plugins.withId("org.openapi.generator") {
    tasks.configureEach {
        if (!isOpenApiGenerateTask(this)) {
            return@configureEach
        }

        additionalPropertiesOf(this).putAll(
            generatorNameOf(this).map { generatorName ->
                if (generatorName == "spring") springJacksonAnnotationOptions else emptyMap()
            })
    }
}
