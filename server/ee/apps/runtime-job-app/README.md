# Runtime Job Application

The Runtime Job Application is a standalone Spring Boot application that executes ByteChef workflows programmatically. It's designed to run workflows outside of the main ByteChef platform, making it suitable for batch processing, scheduled jobs, or integration with external systems.

## Overview

The Runtime Job Application (`RuntimeJobApplication`) is a command-line application that:
- Executes ByteChef workflows from JSON files
- Supports parameterized workflow execution
- Handles connection configurations for external services
- Provides logging and monitoring capabilities

## Prerequisites

- Java 25 or higher
- Access to workflow JSON files
- Connection credentials for external services (if required by workflows)

## Command Line Arguments

The application accepts the following command line arguments:

### Required Arguments

- `--workflow=<workflow_file>`: The workflow to run, given as a file name with its extension, e.g. `my-workflow.json`.

  This is a **lookup key, not a path**. The directory part of the value is discarded and the
  remaining base name, minus the extension, is base64-encoded into the workflow id - so
  `--workflow=/anything/at/all/my-workflow.json` and `--workflow=my-workflow.json` are equivalent.
  The file itself must be discoverable through one of the configured workflow repositories, see
  [Where workflows are loaded from](#where-workflows-are-loaded-from).

### Optional Arguments

- `--parameters=<json_string>`: JSON string containing workflow input parameters
- `--connections=<json_string>`: JSON string containing connection configurations
- `--timeout=<duration>`: Maximum time to wait for the workflow to finish, e.g. `30s`, `15m`, `1h`. When it elapses the job is abandoned and the process exits with code `1`. Without it the app waits indefinitely.

## Where Workflows Are Loaded From

At startup the app scans the enabled workflow repositories and indexes every file it finds under
`base64(file name without extension)`. `--workflow` then looks that id up. If nothing was indexed
under the id, the run fails with `Workflow with id: <base64> does not exist`.

| Repository | Enabled by default | Location |
|---|---|---|
| Filesystem | Yes | `${user.home}/bytechef/data/workflows/*.{json\|yml\|yaml}` - inside the container that is `/root/bytechef/data/workflows`, since the image runs as root |
| Classpath | Yes | `workflows/*.{json\|yml\|yaml}` from inside the JAR |
| Git | No | `BYTECHEF_WORKFLOW_REPOSITORY_GIT_ENABLED=true` plus the `..._GIT_URL` / `..._GIT_BRANCH` / `..._GIT_SEARCH_PATHS` settings |

Override the filesystem location with `BYTECHEF_WORKFLOW_REPOSITORY_FILESYSTEM_LOCATIONPATTERN`
(the `..._LOCATION_PATTERN` spelling binds too). So to run a file that lives somewhere else:

```bash
BYTECHEF_WORKFLOW_REPOSITORY_FILESYSTEM_LOCATIONPATTERN='/tmp/my-workflows/*.json' \
  java -jar runtime-job-app.jar --workflow=my-workflow.json
```

## Exit Codes

The app blocks until the job reaches a terminal status, then shuts down and exits:

| Exit code | Meaning |
|---|---|
| `0` | The job completed |
| `1` | The job failed or was stopped, or `--timeout` elapsed before the job finished |
| non-zero | The app could not start - missing `--workflow`, unparseable `--parameters` / `--connections` JSON, or a workflow that cannot be resolved |

## Usage Examples

These assume the workflow file sits in `~/bytechef/data/workflows/`, the default filesystem
location. See [Where workflows are loaded from](#where-workflows-are-loaded-from) otherwise.

### Basic Workflow Execution

```bash
java -jar runtime-job-app.jar --workflow=my-workflow.json
```

### Workflow with Parameters

```bash
java -jar runtime-job-app.jar \
  --workflow=my-workflow.json \
  --parameters='{"inputValue": "Hello World", "count": 5}'
```

### Workflow with Connections

```bash
java -jar runtime-job-app.jar \
  --workflow=my-workflow.json \
  --connections='{"openAi": {"token": "your-api-key"}, "database": {"url": "jdbc:postgresql://localhost:5432/mydb", "username": "user", "password": "pass"}}'
```

### Complete Example

```bash
java -jar runtime-job-app.jar \
  --workflow=workflow1.json \
  --parameters='{"message": "Processing batch job"}' \
  --connections='{"openAi": {"token": "sk-your-openai-token"}}'
```

## Workflow File Format

> **Export workflows from the ByteChef editor rather than hand-writing them.** Component property
> defaults are materialized into the workflow JSON by the editor when a node is added; nothing fills
> them in at execution time. A hand-written or outdated file that omits a required property fails
> mid-run with `Unknown value for : <property>` - for example an `openAi/v1/ask` task missing
> `"format": "SIMPLE"` or `"format": "ADVANCED"`.

Workflows must be in JSON format with the following structure:

```json
{
  "label": "My Workflow",
  "description": "Description of the workflow",
  "inputs": [],
  "triggers": [
    {
      "description": "",
      "label": "Manual",
      "name": "trigger_1",
      "type": "manual/v1/manual"
    }
  ],
  "tasks": [
    {
      "label": "Task Name",
      "name": "task_1",
      "parameters": {
        "param1": "value1",
        "param2": "value2"
      },
      "type": "component/v1/action",
      "metadata": {
        "ui": {
          "dynamicPropertyTypes": {
            "param1": "STRING"
          }
        }
      }
    }
  ]
}
```

## Connection Configuration

Connections are configured using JSON format where each key represents a connection name and the value contains the connection parameters:

```json
{
  "connectionName": {
    "parameter1": "value1",
    "parameter2": "value2"
  }
}
```

### Common Connection Examples

#### OpenAI Connection
```json
{
  "openAi": {
    "token": "sk-your-openai-api-key"
  }
}
```

#### Database Connection
```json
{
  "database": {
    "url": "jdbc:postgresql://localhost:5432/mydb",
    "username": "dbuser",
    "password": "dbpass"
  }
}
```

#### HTTP Service Connection
```json
{
  "apiService": {
    "baseUrl": "https://api.example.com",
    "apiKey": "your-api-key",
    "timeout": 30000
  }
}
```

## Connection Mapping

For connection names, **workflow task names can be used besides component names**. The `RuntimeTaskDispatcherPreSendProcessor` handles connection mapping in the following order:

1. **Task Name Mapping**: First tries to match connection by the workflow task's name
2. **Component Name Mapping**: If no task name match, tries to match by component name
3. **No Connection**: If no match found, the task runs without connection parameters

### Connection Name Options

You have two options for specifying connection names:

#### Option 1: Using Workflow Task Names (Recommended for specific tasks)
```json
{
  "openAi_1": {
    "token": "sk-your-openai-token-for-task1"
  },
  "openAi_2": {
    "token": "sk-your-openai-token-for-task2"
  }
}
```

#### Option 2: Using Component Names (Shared across all tasks of same type)
```json
{
  "openAi": {
    "token": "sk-your-shared-openai-token"
  }
}
```

### Mapping Example

For a task named `openAi_1` of type `openAi/v1/ask`, the processor will:
1. Look for a connection named `openAi_1` (workflow task name)
2. If not found, look for a connection named `openAi` (component name)
3. If neither found, run without connection

This allows you to have different connection configurations for different tasks of the same component type, or use a shared connection configuration for all tasks of a component.

## Running the Application

### Using Java directly

```bash
# Build the application first
./gradlew :server:ee:apps:runtime-job-app:build

# Run the application
java -jar server/ee/apps/runtime-job-app/build/libs/runtime-job-app.jar \
  --workflow=workflow.json \
  --parameters='{"key": "value"}' \
  --connections='{"service": {"token": "your-token"}}'
```

### Using Gradle

```bash
./gradlew :server:ee:apps:runtime-job-app:bootRun --args='--workflow=workflow.json --connections={"openAi":{"token":"test-token"}}'
```

### Using Docker

The app is published as `bytechef/bytechef-runtime-job`.

#### Where the container looks for workflows

The `--workflow` value is resolved to a workflow id from the file name alone, so the
directory part of the value is ignored and the extension is required. The file itself
must be discoverable through one of the configured workflow repository locations. Point
the filesystem location at your mount:

```bash
# Basic workflow execution
docker run --rm \
  -v $(pwd)/workflows:/workflows \
  -e BYTECHEF_WORKFLOW_REPOSITORY_FILESYSTEM_LOCATION_PATTERN='/workflows/*.json' \
  bytechef/bytechef-runtime-job \
  --workflow=my-workflow.json

# Workflow with parameters
docker run --rm \
  -v $(pwd)/workflows:/workflows \
  -e BYTECHEF_WORKFLOW_REPOSITORY_FILESYSTEM_LOCATION_PATTERN='/workflows/*.json' \
  bytechef/bytechef-runtime-job \
  --workflow=my-workflow.json \
  --parameters='{"inputValue": "Hello World", "count": 5}'

# Workflow with connections
docker run --rm \
  -v $(pwd)/workflows:/workflows \
  -e BYTECHEF_WORKFLOW_REPOSITORY_FILESYSTEM_LOCATION_PATTERN='/workflows/*.json' \
  bytechef/bytechef-runtime-job \
  --workflow=my-workflow.json \
  --connections='{"openAi": {"token": "your-api-key"}}'
```

Without the override the app reads its default filesystem location, which inside the
container is `/root/bytechef/data/workflows`. Mounting there works just as well:

```bash
docker run --rm \
  -v $(pwd)/workflows:/root/bytechef/data/workflows \
  bytechef/bytechef-runtime-job \
  --workflow=my-workflow.json
```

#### Building the image yourself

```bash
./gradlew :server:ee:apps:runtime-job-app:build
docker build -t bytechef/bytechef-runtime-job server/ee/apps/runtime-job-app/
```

The release script at the repository root builds and pushes this image when given the
`--runtime-job` flag (without it, the script builds the `bytechef/bytechef` image instead):

```bash
./docker-build.sh --runtime-job 20260909
```

**Note**: To add JVM options, set `JAVA_TOOL_OPTIONS` on the container, for example
`docker run --rm -e JAVA_TOOL_OPTIONS=-Xmx2g bytechef/bytechef-runtime-job --workflow=my-workflow.json`.

**Note**: The image is built with the `prod` profile, so it logs in ECS structured JSON format.
OTLP export is **off by default** for metrics, tracing and logging alike; turn it on per signal with
`BYTECHEF_OBSERVABILITY_METRICS_ENABLED` / `..._TRACING_ENABLED` / `..._LOGGING_ENABLED`. See the
[Runtime Job Runner](https://docs.bytechef.io/platform/use-bytechef/self-hosted/runtime-job)
documentation for the full environment variable reference.

## Environment Configuration

The application supports different profiles and configurations:

- **Development**: Use `application-dev.yml` for development settings
- **Production**: Configure appropriate logging levels and resource limits

### Logging Configuration

The application uses SLF4J with Logback. Default logging levels:
- ROOT: INFO
- com.bytechef: DEBUG (in development)

## Error Handling

The application will exit with an error if:
- No workflow argument is provided
- The workflow file cannot be found or parsed
- Required connections are missing for workflow tasks
- Workflow execution fails

## Troubleshooting

### Common Issues

1. **"Workflow name is required"**: Ensure the `--workflow` argument is provided
2. **`Workflow with id: <base64> does not exist`**: no repository indexed a file under that name.
   The id is `base64(file name without extension)`. Check that the file really sits in the
   configured location - inside a container, that the directory is actually mounted - that its
   extension matches the location pattern, and remember that the directory part of `--workflow` is
   ignored, so passing a host path does not make the file reachable.
3. **`Unknown value for : <property>`**: the workflow JSON omits a component property the action
   requires. Re-export the workflow from the ByteChef editor rather than adding the key by hand.
4. **Connection errors**: Verify connection parameters and network connectivity
5. **JSON parsing errors**: Validate JSON format for parameters and connections
6. **The run never finishes**: the app waits indefinitely for a terminal status. Pass `--timeout`
   to bound it.

### Debug Mode

Enable debug logging by setting the appropriate log level:

```bash
java -jar runtime-job-app.jar \
  --workflow=workflow.json \
  --logging.level.com.bytechef=DEBUG
```

## Related Components

- **RuntimeJobApplication**: Main application class that handles command line arguments and workflow execution
- **RuntimeTaskDispatcherPreSendProcessor**: Handles connection parameter mapping for workflow tasks
- **JobRunner**: Executes the actual workflow logic

## License

This application is part of ByteChef Enterprise Edition and is licensed under the ByteChef Enterprise License.
