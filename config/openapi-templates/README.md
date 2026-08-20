# Vendored openapi-generator templates

`pojo.mustache` is openapi-generator **7.24.0**'s `JavaSpring/pojo.mustache` with the two
`@JsonInclude`/`@JsonSetter(nulls = SKIP)` blocks removed, so REST models keep emitting explicit
`"field": null` the way they did before 7.24.0. There is no generator flag for that —
`SpringCodegen#postProcessModelProperty` adds the annotations unconditionally — which is why a whole
template is vendored to change two blocks.

**This directory is wired repo-wide, not per module.** The `com.bytechef.openapi-generator-conventions`
convention plugin (`buildSrc/src/main/kotlin/`) sets it as the `templateDir` convention of every
`GenerateTask` whose `generatorName` is `spring`, and root's `subprojects { apply(...) }` applies that
plugin everywhere — so a module is covered by registering a spring generator task, with nothing to opt
into. Tasks using any other generator (`typescript-fetch`, `java`) are left alone; they do not read
`pojo.mustache`. See that plugin for the full reasoning.

`pojo.mustache` sits at the top of this directory, **not** under a `JavaSpring/` subdirectory:
openapi-generator resolves `templateDir` entries flat, the same layout its own
`author template -g spring` extraction produces.

**It is pinned to 7.24.0's shape.** Every fix a later generator makes to this template stops reaching
our models the moment we upgrade, silently. So on any openapi-generator bump, re-diff this file
against the new generator's own `JavaSpring/pojo.mustache` (extract it with
`openapi-generator author template -g spring`, or read it out of the cached jar) and re-apply the two
deletions on top of the new version — do not keep this copy.

The `verifyOpenApiPojoTemplate` Gradle task (root project, wired into `check`) turns that obligation
into a red build rather than a debugging session: it reads the generator's own template out of the
resolved `org.openapitools:openapi-generator` artifact, applies the two deletions, and fails when the
result no longer equals this file. Its failure message carries the fix procedure.

Only files present here are overridden; everything else falls back to the generator's embedded
templates. Keep this directory as small as it is.
