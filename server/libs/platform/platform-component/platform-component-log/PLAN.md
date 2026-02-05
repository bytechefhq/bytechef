# Component Execution Log Storage

## Overview

This module provides structured logging for component execution during workflow runs. Logs are stored per workflow execution (jobId) and can be queried via GraphQL.

## Module Structure

```
platform-component-log/
├── platform-component-log-api/           # Interface and domain
│   └── src/main/java/com/bytechef/platform/component/log/
│       ├── LogFileStorage.java           # Storage interface
│       └── domain/
│           └── LogEntry.java             # Log entry record
│
├── platform-component-log-service/       # Implementations
│   └── src/main/java/com/bytechef/platform/component/log/
│       ├── LogFileStorageImpl.java       # Persistent storage (FileStorageService)
│       ├── TempFileLogFileStorage.java   # Temp file storage (editor/test)
│       └── config/
│           └── LogFileStorageConfiguration.java  # Spring configuration
│
└── platform-component-log-graphql/       # GraphQL API
    └── src/
        ├── main/java/com/bytechef/platform/component/log/web/graphql/
        │   └── LogGraphQlController.java
        └── main/resources/graphql/
            └── component-log.graphqls
```

## Components

### LogEntry (Domain)

A record representing a single log entry:

```java
public record LogEntry(
    Instant timestamp,
    Level level,                    // TRACE, DEBUG, INFO, WARN, ERROR
    String componentName,
    @Nullable String componentOperationName,
    long taskExecutionId,
    String message,
    @Nullable String exceptionType,
    @Nullable String exceptionMessage,
    @Nullable String stackTrace
)
```

### LogFileStorage (Interface)

```java
public interface LogFileStorage {
    void storeLogEntry(long jobId, long taskExecutionId, LogEntry logEntry);
    void storeLogEntries(long jobId, long taskExecutionId, List<LogEntry> logEntries);
    void flushLogs(long jobId);
    List<LogEntry> readLogEntries(long jobId, long taskExecutionId);
    List<LogEntry> readLogEntriesByJobId(long jobId);
    List<LogEntry> readLogEntriesByJobId(long jobId, LogEntry.Level minLevel);
    boolean logsExist(long jobId);
    void deleteLogEntries(long jobId);
}
```

### Implementations

#### LogFileStorageImpl (Production)

- Uses `FileStorageService` for persistent storage
- Directory: `logs/component_execution`
- File naming: `{jobId}.json`
- Compresses JSON using `CompressionUtils`
- Buffers logs in memory during execution, flushes on job completion

#### TempFileLogFileStorage (Editor/Test)

- Creates temp directory: `Files.createTempDirectory("bytechef_logs_")`
- Each job's logs stored in: `job_{jobId}_{random}.json`
- Logs buffered and flushed to temp file on each store operation
- `cleanupJobStorage()` deletes temp files when job completes

### GraphQL API

There are two GraphQL controllers:
1. **LogGraphQlController** - For production logs stored via `LogFileStorageImpl`
2. **TempLogGraphQlController** - For editor/test logs stored via `TempFileLogFileStorage`

#### Schema (component-log.graphqls)

```graphql
enum LogLevel { TRACE, DEBUG, INFO, WARN, ERROR }

type LogEntry {
    timestamp: String!
    level: LogLevel!
    componentName: String!
    componentOperationName: String
    taskExecutionId: ID!
    message: String!
    exceptionType: String
    exceptionMessage: String
    stackTrace: String
}

type LogPage {
    content: [LogEntry!]!
    totalElements: Int!
    totalPages: Int!
    pageNumber: Int!
    pageSize: Int!
    hasNext: Boolean!
    hasPrevious: Boolean!
}

input LogFilterInput {
    minLevel: LogLevel
    componentName: String
    taskExecutionId: ID
    fromTimestamp: String
    toTimestamp: String
    searchText: String
}

extend type Query {
    jobLogs(jobId: ID!, filter: LogFilterInput, page: Int, size: Int): LogPage!
    taskExecutionLogs(jobId: ID!, taskExecutionId: ID!): [LogEntry!]!
    jobLogsExist(jobId: ID!): Boolean!
}

extend type Mutation {
    deleteJobLogs(jobId: ID!): Boolean!
}
```

#### Temp Log Schema (temp-component-log.graphqls)

```graphql
extend type Query {
    tempJobLogs(jobId: ID!, filter: LogFilterInput, page: Int, size: Int): LogPage!
    tempTaskExecutionLogs(jobId: ID!, taskExecutionId: ID!): [LogEntry!]!
    tempJobLogsExist(jobId: ID!): Boolean!
}

extend type Mutation {
    deleteTempJobLogs(jobId: ID!): Boolean!
    cleanupTempJobStorage(jobId: ID!): Boolean!
}
```

## Integration

### ContextImpl.LogImpl

Modified to capture logs and store them via `LogFileStorage` while continuing to log via SLF4J:

```java
private static class LogImpl implements Log {
    private final String componentName;
    private final @Nullable String componentOperationName;
    private final @Nullable Long jobId;
    private final @Nullable LogFileStorage logFileStorage;
    private final org.slf4j.Logger logger;
    private final long taskExecutionId;

    // Each log method (debug, info, warn, error, trace) now also stores to LogFileStorage
    private void storeLogEntry(LogEntry.Level level, String message, @Nullable Exception exception) {
        if (logFileStorage != null && jobId != null) {
            LogEntry.Builder builder = LogEntry.builder()
                .timestamp(Instant.now())
                .level(level)
                .componentName(componentName)
                .componentOperationName(componentOperationName)
                .taskExecutionId(taskExecutionId)
                .message(message);

            if (exception != null) {
                builder.exception(exception);
            }

            logFileStorage.storeLogEntry(jobId, taskExecutionId, builder.build());
        }
    }
}
```

### ContextFactoryImpl

- Injects `LogFileStorage` bean for production
- Creates `TempFileLogFileStorage` instance for editor environments
- Passes appropriate storage to `ActionContextImpl`

```java
private LogFileStorage getLogFileStorage(boolean editorEnvironment) {
    if (editorEnvironment) {
        return editorLogFileStorage;  // TempFileLogFileStorage
    }
    return logFileStorage;  // LogFileStorageImpl
}
```

## JSON Log Format

```json
[
  {
    "timestamp": "2026-01-20T10:30:45.123Z",
    "level": "INFO",
    "componentName": "http",
    "componentOperationName": "get",
    "taskExecutionId": 12345,
    "message": "Sending GET request to https://api.example.com"
  },
  {
    "timestamp": "2026-01-20T10:30:46.789Z",
    "level": "ERROR",
    "componentName": "slack",
    "componentOperationName": "sendMessage",
    "taskExecutionId": 12346,
    "message": "Failed to send message",
    "exceptionType": "com.bytechef.component.exception.ProviderException",
    "exceptionMessage": "Channel not found",
    "stackTrace": "..."
  }
]
```

## Dependencies

### platform-component-log-api

```kotlin
dependencies {
    api(project(":server:libs:core:file-storage:file-storage-api"))
    implementation("org.jspecify:jspecify")
}
```

### platform-component-log-service

```kotlin
dependencies {
    api(project(":server:libs:platform:platform-component:platform-component-log:platform-component-log-api"))
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
```

### platform-component-log-graphql

```kotlin
dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-log:platform-component-log-api"))
}
```

## Usage Examples

### Query all logs for a job

```graphql
query {
  jobLogs(jobId: "123") {
    content {
      timestamp
      level
      componentName
      message
    }
    totalElements
  }
}
```

### Query with filtering

```graphql
query {
  jobLogs(jobId: "123", filter: { minLevel: WARN, componentName: "http" }, page: 0, size: 50) {
    content {
      timestamp
      level
      message
      exceptionMessage
    }
    totalElements
    hasNext
  }
}
```

### Query logs for a specific task

```graphql
query {
  taskExecutionLogs(jobId: "123", taskExecutionId: "456") {
    timestamp
    level
    message
  }
}
```

### Delete logs for a job

```graphql
mutation {
  deleteJobLogs(jobId: "123")
}
```

## Temp Log Usage Examples (Editor/Test)

### Query temp logs for an editor test execution

```graphql
query {
  tempJobLogs(jobId: "456") {
    content {
      timestamp
      level
      componentName
      message
    }
    totalElements
  }
}
```

### Query temp logs for a specific task

```graphql
query {
  tempTaskExecutionLogs(jobId: "456", taskExecutionId: "789") {
    timestamp
    level
    message
  }
}
```

### Cleanup temp storage after test

```graphql
mutation {
  cleanupTempJobStorage(jobId: "456")
}
```

## Files Modified

| File | Change |
|------|--------|
| `settings.gradle.kts` | Added 3 new module includes |
| `ContextImpl.java` | Modified LogImpl to capture and store logs |
| `ContextFactoryImpl.java` | Injects LogFileStorage and TempFileLogFileStorage |
| `ActionContextImpl.java` | Updated constructor to pass LogFileStorage to parent |
| `platform-component-context-service/build.gradle.kts` | Added log module dependencies |

## Files Created

| File | Description |
|------|-------------|
| `platform-component-log-api/LogFileStorage.java` | Storage interface |
| `platform-component-log-api/domain/LogEntry.java` | Log entry record with Level enum |
| `platform-component-log-service/LogFileStorageImpl.java` | Persistent storage using FileStorageService |
| `platform-component-log-service/TempFileLogFileStorage.java` | Temp file storage for editor/test environments |
| `platform-component-log-service/config/LogFileStorageConfiguration.java` | Spring bean configuration |
| `platform-component-log-graphql/LogGraphQlController.java` | GraphQL controller for production logs |
| `platform-component-log-graphql/TempLogGraphQlController.java` | GraphQL controller for temp/editor logs |
| `platform-component-log-graphql/resources/graphql/component-log.graphqls` | GraphQL schema for production logs |
| `platform-component-log-graphql/resources/graphql/temp-component-log.graphqls` | GraphQL schema for temp logs |
