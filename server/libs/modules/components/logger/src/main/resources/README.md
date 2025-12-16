# Logger component

The Logger component provides logging functionality for ByteChef workflows, allowing you to output messages at different log levels for debugging, monitoring, and troubleshooting.

## Overview

The Logger component is a utility that enables you to log messages during workflow execution. It supports four standard log levels/actions: `debug`, `info`, `warn`, and `error`. Logs are written to the application logs (via SLF4J) and are also captured in the ByteChef workflow execution logs UI.

## Available actions

### Debug
Logs a debug message. Debug messages are typically used for detailed diagnostic information that are only of interest when diagnosing problems.

**Properties (for all actions):**
- `text` (string, required): The message to log. Supports expressions using `${ ... }` to interpolate runtime values.

**Example:**
```json
{
  "text": "Processing user data for ID: ${trigger.userId}"
}
```

### Info
Logs an informational message. Info messages record general information about the workflow execution.

**Example:**
```json
{
  "text": "Workflow started successfully at =now()"
}
```

### Warn
Logs a warning message. Warning messages indicate potential issues or unexpected conditions that don't prevent the workflow from continuing.

**Example:**
```json
{
  "text": "API rate limit approaching: ${api.remainingRequests} requests left"
}
```

### Error
Logs an error message. Error messages indicate serious problems that may cause the workflow to fail or behave unexpectedly.

**Example:**
```json
{
  "text": "Failed to process payment: ${error.message}"
}
```

## Usage examples

### Basic logging
Use the Logger component to track workflow progress:

```yaml
# Log workflow start
- component: logger
  action: info
  text: "Starting data synchronization workflow"

# Log processing steps
- component: logger
  action: debug
  text: "Processing ${items.length} items"

# Log completion
- component: logger
  action: info
  text: "Workflow completed successfully"
```

### Error handling and debugging
Use different log levels for comprehensive error tracking:

```yaml
# Log successful operations
- component: logger
  action: info
  text: "Successfully created {{createdCount}} records"

# Log warnings for non-critical issues
- component: logger
  action: warn
  text: "Some records were skipped due to validation errors"

# Log detailed debug information
- component: logger
  action: debug
  text: "Processing record ${record.id} with data: ${record.data}"

# Log errors for critical failures
- component: logger
  action: error
  text: "Failed to connect to database: ${error.message}"
```

### Conditional logging
Use the Logger with conditional logic for smart logging:

```yaml
# Only log debug info when in development mode
- component: logger
  action: debug
  text: "Debug: Processing user ${user.email}"
  condition: "${env.ENVIRONMENT} == 'development'"

# Log warnings for high usage
- component: logger
  action: warn
  text: "High API usage detected: ${api.usage}% of limit"
  condition: "${api.usage} > 80"
```

## Best practices

### Log level guidelines
- **Debug**: Use for detailed diagnostic information, variable values, and step-by-step execution details
- **Info**: Use for general workflow progress, successful operations, and important state changes
- **Warn**: Use for potential issues, deprecated features, or unexpected but recoverable conditions
- **Error**: Use for serious problems, failures, and conditions that prevent normal operation

### Message content
- **Be descriptive**: Include context about what the workflow is doing
- **Include relevant data**: Use expressions to include dynamic values like IDs, counts, or status
- **Use consistent formatting**: Follow a consistent message format across your workflows
- **Avoid sensitive data**: Never log passwords, API keys, or personal information

### Performance considerations
- **Use appropriate log levels**: Avoid excessive debug logging in production
- **Keep messages concise**: Long log messages can impact performance
- **Use expressions efficiently**: Complex expressions in log messages can slow down execution

## Integration with other components

The Logger component works well with:

- **Error handling**: Use with Try/Catch components to log errors
- **Conditional flows**: Use with If/Switch components for conditional logging
- **Data processing**: Log progress during data transformation workflows
- **API integrations**: Log API responses and status codes
- **Scheduled workflows**: Log execution times and results

## Troubleshooting

### Common issues

**Logs not appearing:**
- Check the log level configuration in your ByteChef instance
- Ensure the Logger component is properly configured
- Verify that the workflow execution completed successfully

**Performance impact:**
- Reduce the frequency of debug logging in production
- Use conditional logging to avoid unnecessary log entries
- Consider using `info` level instead of `debug` for frequent operations

### Viewing logs
- Logs are available in the ByteChef workflow execution history
- Use the workflow execution details to view all log entries
- Logs are timestamped and include the workflow execution ID

## Technical details

- **Component category**: Helpers
- **Connection**: Not required — the Logger component doesn't require any external connections
- **Return value**: All Logger actions return `null` (side-effect only)
- **Thread safety**: Logger actions are thread-safe and can be used in parallel workflows
- **Log format**: Messages are logged using the standard SLF4J logging framework

## Notes
- Action names are lowercase: `debug`, `info`, `warn`, `error`.
- The `text` property supports expression interpolation with `${ }`. Avoid logging secrets or personal data.
