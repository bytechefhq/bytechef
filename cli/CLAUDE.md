# CLI
ByteChef includes a CLI (`cli/`, Spring Boot + Spring Shell) that scaffolds custom components and
calls the public REST API (automation and embedded). It uses the `application` plugin. Build a
`bytechef` binary with `installDist` — this is the normal way to use it, since the binary runs from
your current working directory so relative paths behave as expected:
```bash
# Build the distribution; binary lands at cli/cli-app/build/install/bytechef/bin/bytechef
./gradlew :cli:cli-app:installDist

# Scaffold a component from an OpenAPI spec
bytechef component init --name my-component --open-api-path ./openapi.yaml --output-path .

# Public API commands (store a profile first)
bytechef configure --host https://app.bytechef.io --token <public-api-token> --environment PRODUCTION --workspace-id 1
bytechef automation project pull --id 5
bytechef embedded integration list --external-user-id user-42
```
For a quick dev invocation without building the distribution, use the `run` task — but note its
working directory is `cli/cli-app`, so pass **absolute** paths:
```bash
./gradlew :cli:cli-app:run --args="component init --name my-component --open-api-path /abs/openapi.yaml --output-path /abs/out"
```
See `cli/README.md` for the full command reference.

Regenerate the CLI's OpenAPI clients with each project's **`generateClient`** task — not the plugin's
default `openApiGenerate`, which is unconfigured and fails with "generator name must be specified".
Generated Java sources are committed and deliberately not wired to `compileJava`; regenerate manually
when an `openapi.yaml` changes. The surrounding `docs/`, `gradlew`, `pom.xml` scaffolding is gitignored.
