# Logger Component

The Logger component provides logging functionality for ByteChef workflows, allowing you to output messages at different log levels for debugging, monitoring, and troubleshooting purposes.

## Overview

The Logger component is a utility component that enables you to log messages during workflow execution. It supports four standard log levels: Debug, Info, Warn, and Error. These logs are written to the system log and can be viewed in the ByteChef execution logs.

## Available Actions

### Debug
Logs a debug message. Debug messages are typically used for detailed diagnostic information that is only of interest when diagnosing problems.

**Properties:**
- `text` (string, required): The message to log

**Example:**
```json
{
  "text": "Processing user data for ID: {{trigger.userId}}"
}
```

### Info
Logs an informational message. Info messages are used to record general information about the workflow execution.

**Properties:**
- `text` (string, required): The message to log

**Example:**
```json
{
  "text": "Workflow started successfully at {{now()}}"
}
```

### Warn
Logs a warning message. Warning messages indicate potential issues or unexpected conditions that don't prevent the workflow from continuing.

**Properties:**
- `text` (string, required): The message to log

**Example:**
```json
{
  "text": "API rate limit approaching: {{api.remainingRequests}} requests left"
}
```

### Error
Logs an error message. Error messages indicate serious problems that may have caused the workflow to fail or behave unexpectedly.

**Properties:**
- `text` (string, required): The message to log

**Example:**
```json
{
  "text": "Failed to process payment: {{error.message}}"
}
```

## Usage Examples

### Basic Logging
Use the Logger component to track workflow progress:

```yaml
# Log workflow start
- component: logger
  action: info
  text: "Starting data synchronization workflow"

# Log processing steps
- component: logger
  action: debug
  text: "Processing {{items.length}} items"

# Log completion
- component: logger
  action: info
  text: "Workflow completed successfully"
```

### Error Handling and Debugging
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
  text: "Processing record {{record.id}} with data: {{record.data}}"

# Log errors for critical failures
- component: logger
  action: error
  text: "Failed to connect to database: {{error.message}}"
```

### Conditional Logging
Use Logger with conditional logic for smart logging:

```yaml
# Only log debug info when in development mode
- component: logger
  action: debug
  text: "Debug: Processing user {{user.email}}"
  condition: "{{env.ENVIRONMENT}} == 'development'"

# Log warnings for high usage
- component: logger
  action: warn
  text: "High API usage detected: {{api.usage}}% of limit"
  condition: "{{api.usage}} > 80"
```

## Best Practices

### Log Level Guidelines
- **Debug**: Use for detailed diagnostic information, variable values, and step-by-step execution details
- **Info**: Use for general workflow progress, successful operations, and important state changes
- **Warn**: Use for potential issues, deprecated features, or unexpected but recoverable conditions
- **Error**: Use for serious problems, failures, and conditions that prevent normal operation

### Message Content
- **Be descriptive**: Include context about what the workflow is doing
- **Include relevant data**: Use expressions to include dynamic values like IDs, counts, or status
- **Use consistent formatting**: Follow a consistent message format across your workflows
- **Avoid sensitive data**: Never log passwords, API keys, or personal information

### Performance Considerations
- **Use appropriate log levels**: Avoid excessive debug logging in production
- **Keep messages concise**: Long log messages can impact performance
- **Use expressions efficiently**: Complex expressions in log messages can slow down execution

## Integration with Other Components

The Logger component works well with:

- **Error handling**: Use with Try/Catch components to log errors
- **Conditional flows**: Use with If/Switch components for conditional logging
- **Data processing**: Log progress during data transformation workflows
- **API integrations**: Log API responses and status codes
- **Scheduled workflows**: Log execution times and results

## Troubleshooting

### Common Issues

**Logs not appearing:**
- Check the log level configuration in your ByteChef instance
- Ensure the Logger component is properly configured
- Verify that the workflow execution completed successfully

**Performance impact:**
- Reduce the frequency of debug logging in production
- Use conditional logging to avoid unnecessary log entries
- Consider using Info level instead of Debug for frequent operations

### Viewing Logs
- Logs are available in the ByteChef execution history
- Use the workflow execution details to view all log entries
- Logs are timestamped and include the workflow execution ID

## Technical Details

- **Component Category**: Helpers
- **No Connection Required**: The Logger component doesn't require any external connections
- **Return Value**: All Logger actions return `null`
- **Thread Safety**: Logger actions are thread-safe and can be used in parallel workflows
- **Log Format**: Messages are logged using the standard SLF4J logging framework
