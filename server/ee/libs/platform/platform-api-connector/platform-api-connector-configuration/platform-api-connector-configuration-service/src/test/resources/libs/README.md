# Compile classpath for the legacy generator reference

These jars are the javac classpath `OpenApiGenerator` (test scope) uses when compiling the sources the CLI generator
emits inside `OpenApiComponentDefinitionFactoryTest`'s golden comparisons. They only gate compilation — at runtime the
compiled classes link against the current SDK on the test classpath, so the serialized definitions being compared
always come from the live `ComponentDsl`.

Refresh them after SDK signature changes that the CLI generator's emitted code depends on:

```
./gradlew :sdks:backend:java:component-api:jar :sdks:backend:java:definition-api:jar
cp sdks/backend/java/component-api/build/libs/component-api-1.0.jar .
cp sdks/backend/java/definition-api/build/libs/definition-api-1.0.jar .
```

`auto-service-annotations` is the plain Maven artifact and rarely needs touching.
