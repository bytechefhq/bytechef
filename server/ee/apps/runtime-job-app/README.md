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

- `--workflow=<workflow_file>`: Path to the workflow JSON file to execute

### Optional Arguments

- `--parameters=<json_string>`: JSON string containing workflow input parameters
- `--connections=<json_string>`: JSON string containing connection configurations

## Usage Examples

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
java -jar server/ee/apps/runtime-job-app/build/libs/runtime-job-app-*.jar \
  --workflow=path/to/workflow.json \
  --parameters='{"key": "value"}' \
  --connections='{"service": {"token": "your-token"}}'
```

### Using Gradle

```bash
./gradlew :server:ee:apps:runtime-job-app:bootRun --args='--workflow=workflow.json --connections={"openAi":{"token":"test-token"}}'
```

### Using Docker

First, build the Docker image:

```bash
# Build the application and Docker image
./gradlew :server:ee:apps:runtime-job-app:build
docker build -t bytechef-runtime-job server/ee/apps/runtime-job-app/
```

Then run the container with your workflow and arguments:

```bash
# Basic workflow execution
docker run --rm \
  -v /path/to/your/workflows:/workflows \
  bytechef-runtime-job \
  --workflow=/workflows/my-workflow.json

# Workflow with parameters
docker run --rm \
  -v /path/to/your/workflows:/workflows \
  bytechef-runtime-job \
  --workflow=/workflows/my-workflow.json \
  --parameters='{"inputValue": "Hello World", "count": 5}'

# Workflow with connections
docker run --rm \
  -v /path/to/your/workflows:/workflows \
  bytechef-runtime-job \
  --workflow=/workflows/my-workflow.json \
  --connections='{"openAi": {"token": "your-api-key"}}'

# Complete example with all arguments
docker run --rm \
  -v /path/to/your/workflows:/workflows \
  bytechef-runtime-job \
  --workflow=/workflows/workflow1.json \
  --parameters='{"message": "Processing batch job"}' \
  --connections='{"openAi": {"token": "sk-your-openai-token"}}'
```

**Note**: The `-v` flag mounts your local workflow directory into the container so the application can access your workflow files.

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
2. **Connection errors**: Verify connection parameters and network connectivity
3. **JSON parsing errors**: Validate JSON format for parameters and connections
4. **Workflow file not found**: Check file paths and permissions

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
