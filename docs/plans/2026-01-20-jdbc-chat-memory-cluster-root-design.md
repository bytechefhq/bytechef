# JDBC Chat Memory as Cluster Root Element

## Overview

Reimplement `JdbcChatMemoryComponentHandler` as a Cluster Root Element, following the pattern established by `VectorStoreChatMemoryComponentHandler`. The JDBC Chat Memory will consume cluster element child `DataSourceFunction.DATA_SOURCE`, where PostgreSQL, MySQL, and Oracle components provide the DataSource cluster elements.

## Design Decisions

1. **Extend JdbcComponentHandlerImpl** - Add `DataSourceFunction.DATA_SOURCE` cluster element so all JDBC components automatically get it
2. **Move chat memory actions to JdbcComponentHandlerImpl** - Actions become available on all JDBC components
3. **Use Spring AI JdbcChatMemoryRepository** - Actions use the existing Spring AI repository pattern

## Implementation

### 1. DataSourceFunction Cluster Element in JdbcComponentHandlerImpl

**File:** `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/jdbc/handler/JdbcComponentHandlerImpl.java`

Add a new cluster element of type `DataSourceFunction.DATA_SOURCE` that provides a `javax.sql.DataSource` based on connection parameters. All JDBC components (PostgreSQL, MySQL, Oracle) automatically inherit this capability.

```java
// Add alongside JdbcItemWriter cluster element in getComponentDefinition():
clusterElement("dataSource")
    .title("Data Source")
    .description("Provides a JDBC DataSource for database connections.")
    .type(DataSourceFunction.DATA_SOURCE)
    .object(() -> (inputParameters, connectionParameters, extensions, componentConnections) ->
        DataSourceFactory.getDataSource(connectionParameters, urlTemplate, jdbcDriverClassName))
```

### 2. Chat Memory Actions in JdbcComponentHandlerImpl

**File:** `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/jdbc/handler/JdbcComponentHandlerImpl.java`

Move the following actions from `chat-memory-jdbc` module:
- `chatMemoryAddMessages` - Add messages to conversation
- `chatMemoryGetMessages` - Get messages from conversation
- `chatMemoryDelete` - Delete a conversation
- `chatMemoryListConversations` - List all conversations

Actions use `JdbcChatMemoryRepository` and `JdbcChatMemoryRepositoryDialect` from Spring AI.

**Dependencies to add:**
- Spring AI chat memory dependencies to `platform-component-service` module's `build.gradle`

### 3. JdbcChatMemoryComponentDefinition Interface

**New file:** `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/JdbcChatMemoryComponentDefinition.java`

```java
package com.bytechef.platform.component.definition;

import static com.bytechef.platform.component.definition.ai.agent.DataSourceFunction.DATA_SOURCE;

import com.bytechef.component.definition.ClusterElementDefinition.ClusterElementType;
import java.util.List;

public interface JdbcChatMemoryComponentDefinition extends ClusterRootComponentDefinition {
    @Override
    default List<ClusterElementType> getClusterElementTypes() {
        return List.of(DATA_SOURCE);
    }
}
```

### 4. JdbcChatMemoryComponentHandler as Cluster Root

**File:** `server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/java/com/bytechef/component/ai/agent/chat/memory/jdbc/JdbcChatMemoryComponentHandler.java`

Changes:
- Remove `@AutoService(ComponentHandler.class)` annotation
- Add `@Component(JDBC_CHAT_MEMORY + "_v1_ComponentHandler")` annotation
- Remove `CONNECTION_DEFINITION` entirely
- Remove all action references
- Inject `ClusterElementDefinitionService` via constructor
- Implement `JdbcChatMemoryComponentDefinition` via wrapper class

```java
@Component(JDBC_CHAT_MEMORY + "_v1_ComponentHandler")
public class JdbcChatMemoryComponentHandler implements ComponentHandler {
    public static final String JDBC_CHAT_MEMORY = "jdbcChatMemory";

    private final JdbcChatMemoryComponentDefinition componentDefinition;

    public JdbcChatMemoryComponentHandler(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.componentDefinition = new JdbcChatMemoryComponentDefinitionImpl(
            component(JDBC_CHAT_MEMORY)
                .title("JDBC Chat Memory")
                .description("JDBC Chat Memory stores conversation history in a relational database.")
                .icon("path:assets/jdbc-chat-memory.svg")
                .categories(ComponentCategory.ARTIFICIAL_INTELLIGENCE)
                .clusterElements(new JdbcChatMemory(clusterElementDefinitionService).clusterElementDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class JdbcChatMemoryComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements JdbcChatMemoryComponentDefinition {

        public JdbcChatMemoryComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
```

### 5. JdbcChatMemory Cluster Class Update

**File:** `server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/java/com/bytechef/component/ai/agent/chat/memory/jdbc/cluster/JdbcChatMemory.java`

Changes:
- Convert from static class to instance-based
- Inject `ClusterElementDefinitionService` via constructor
- Get `DataSource` from cluster element child using `ClusterElementMap.of(extensions).getClusterElement(DATA_SOURCE)`

```java
public class JdbcChatMemory {
    public final ClusterElementDefinition<ChatMemoryFunction> clusterElementDefinition;

    private final ClusterElementDefinitionService clusterElementDefinitionService;

    public JdbcChatMemory(ClusterElementDefinitionService clusterElementDefinitionService) {
        this.clusterElementDefinitionService = clusterElementDefinitionService;

        this.clusterElementDefinition = ComponentDsl.<ChatMemoryFunction>clusterElement("chatMemory")
            .title("JDBC Chat Memory")
            .description("Memory is retrieved from a JDBC database and added into the prompt's system text.")
            .type(CHAT_MEMORY)
            .object(() -> this::apply);
    }

    protected PromptChatMemoryAdvisor apply(
        Parameters inputParameters, Parameters connectionParameters, Parameters extensions,
        Map<String, ComponentConnection> componentConnections) throws Exception {

        ClusterElement clusterElement = ClusterElementMap.of(extensions)
            .getClusterElement(DataSourceFunction.DATA_SOURCE);

        DataSourceFunction dataSourceFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = componentConnections.get(clusterElement.getWorkflowNodeName());

        DataSource dataSource = dataSourceFunction.apply(
            ParametersFactory.createParameters(clusterElement.getParameters()),
            ParametersFactory.createParameters(componentConnection.getParameters()),
            ParametersFactory.createParameters(clusterElement.getExtensions()),
            componentConnections);

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        JdbcChatMemoryRepositoryDialect dialect = JdbcChatMemoryRepositoryDialect.from(dataSource);

        JdbcChatMemoryRepository jdbcChatMemoryRepository = JdbcChatMemoryRepository.builder()
            .jdbcTemplate(jdbcTemplate)
            .dialect(dialect)
            .build();

        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
            .chatMemoryRepository(jdbcChatMemoryRepository)
            .build();

        return PromptChatMemoryAdvisor.builder(chatMemory)
            .build();
    }
}
```

## Files to Modify

1. `server/libs/platform/platform-component/platform-component-service/src/main/java/com/bytechef/platform/component/jdbc/handler/JdbcComponentHandlerImpl.java`
2. `server/libs/platform/platform-component/platform-component-service/build.gradle` (add Spring AI dependencies)
3. `server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/java/com/bytechef/component/ai/agent/chat/memory/jdbc/JdbcChatMemoryComponentHandler.java`
4. `server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/java/com/bytechef/component/ai/agent/chat/memory/jdbc/cluster/JdbcChatMemory.java`

## Files to Create

1. `server/libs/platform/platform-component/platform-component-api/src/main/java/com/bytechef/platform/component/definition/JdbcChatMemoryComponentDefinition.java`

## Files to Delete/Deprecate

1. `server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/java/com/bytechef/component/ai/agent/chat/memory/jdbc/action/JdbcChatMemoryAddMessagesAction.java` (move to JdbcComponentHandlerImpl)
2. `server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/java/com/bytechef/component/ai/agent/chat/memory/jdbc/action/JdbcChatMemoryGetMessagesAction.java` (move to JdbcComponentHandlerImpl)
3. `server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/java/com/bytechef/component/ai/agent/chat/memory/jdbc/action/JdbcChatMemoryDeleteAction.java` (move to JdbcComponentHandlerImpl)
4. `server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/java/com/bytechef/component/ai/agent/chat/memory/jdbc/action/JdbcChatMemoryListConversationsAction.java` (move to JdbcComponentHandlerImpl)
5. `server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/java/com/bytechef/component/ai/agent/chat/memory/jdbc/constant/JdbcChatMemoryConstants.java` (move constants to JdbcConstants)
6. `server/libs/modules/components/ai/agent/chat-memory/chat-memory-jdbc/src/main/java/com/bytechef/component/ai/agent/chat/memory/jdbc/util/JdbcChatMemoryUtils.java` (no longer needed)

## Testing

- Update existing tests in `chat-memory-jdbc` module
- Add tests for new actions in `platform-component-service`
- Verify PostgreSQL, MySQL, and Oracle components expose the DataSource cluster element
