# Claude Code Component - Cluster Root Conversion Plan

## Overview

Convert the ClaudeCode component from a simple CLI-wrapper into a cluster-root component (like AiAgent), integrating spring-ai-agent-utils tools as built-in agent capabilities.

## Architecture

### ClusterElementTypes

| Type | Name | Key | Label | Multiple | Required |
|------|------|-----|-------|----------|----------|
| CLAUDE_CODE_MODEL | `CLAUDE_CODE_MODEL` | `claudeCodeModel` | `Model` | false | true |
| CLAUDE_CODE_TOOLS | `CLAUDE_CODE_TOOLS` | `claudeCodeTools` | `Claude Code Tools` | true | false |
| TOOLS | `TOOLS` | `tools` | `Tools` | true | false |

### Module Structure

```
claude-code/
├── src/main/java/com/bytechef/component/claude/code/
│   ├── ClaudeCodeComponentHandler.java        (cluster root, @Component)
│   ├── action/
│   │   └── ClaudeCodeChatAction.java          (Spring AI ChatClient-based)
│   └── constant/
│       └── ClaudeCodeConstants.java
├── claude-code-tool/
│   ├── src/main/java/com/bytechef/component/claude/code/tool/
│   │   ├── ClaudeCodeToolComponentHandler.java
│   │   ├── ClaudeCodeFileSystemTools.java
│   │   ├── ClaudeCodeShellTools.java
│   │   ├── ClaudeCodeGrepTool.java
│   │   ├── ClaudeCodeGlobTool.java
│   │   ├── ClaudeCodeSmartWebFetchTool.java
│   │   ├── ClaudeCodeBraveWebSearchTool.java
│   │   ├── ClaudeCodeSkillsTool.java
│   │   └── ClaudeCodeTodoWriteTool.java
│   └── build.gradle.kts
├── build.gradle.kts
└── src/main/resources/assets/anthropic.svg
```

### New Interface

`ClaudeCodeToolFunction` in `platform-component-api`:

```java
package com.bytechef.platform.component.definition.ai.claudecode;

@FunctionalInterface
public interface ClaudeCodeToolFunction {
    ClusterElementType CLAUDE_CODE_TOOLS = new ClusterElementType(
        "CLAUDE_CODE_TOOLS", "claudeCodeTools", "Claude Code Tools", true, false);

    List<ToolCallback> apply(
        Parameters inputParameters, Parameters connectionParameters,
        Path workingDirectory) throws Exception;
}
```

### Chat Action Flow

```
ClaudeCodeChatAction.perform()
├── Create temp working directory (Files.createTempDirectory)
├── try:
│   ├── Resolve CLAUDE_CODE_MODEL → ChatModel (via ModelFunction)
│   ├── Build ChatClient
│   ├── Resolve CLAUDE_CODE_TOOLS → List<ToolCallback> per tool
│   ├── Resolve TOOLS → List<ToolCallback> (external tools)
│   ├── Combine all ToolCallbacks
│   ├── Execute prompt via ChatClient
│   └── Return response
└── finally:
    └── Delete temp directory
```

---

## Phase 1: Core Infrastructure + Built-in Tools (Current)

### Task 1.1: Add spring-ai-agent-utils dependency
- Add `org.springaicommunity:spring-ai-agent-utils:0.4.2` to `libs.versions.toml`
- Add dependency to `claude-code-tool/build.gradle.kts`

### Task 1.2: Create ClaudeCodeToolFunction interface
- Create `ClaudeCodeToolFunction.java` in `platform-component-api`
- Package: `com.bytechef.platform.component.definition.ai.claudecode`
- Define CLAUDE_CODE_TOOLS ClusterElementType
- Define functional interface: `List<ToolCallback> apply(Parameters, Parameters, Path)`

### Task 1.3: Create claude-code-tool submodule
- Create `claude-code-tool/` directory structure
- Create `build.gradle.kts` with spring-ai-agent-utils dependency
- Register in `settings.gradle.kts`

### Task 1.4: Implement ClaudeCodeToolComponentHandler
- Create `ClaudeCodeToolComponentHandler` with `@AutoService`
- Register all 8 tool cluster element definitions
- Component name: `claudeCodeTool`

### Task 1.5: Implement FileSystemTools cluster element
- Wrap `org.springaicommunity.agent.tools.FileSystemTools`
- Tools: Read, Write, Edit
- Configure with working directory from `Path workingDirectory` parameter
- Return `ToolCallbacks.from(fileSystemTools)`

### Task 1.6: Implement ShellTools cluster element
- Wrap `org.springaicommunity.agent.tools.ShellTools`
- Tools: Bash, BashOutput, KillShell
- Configure with working directory

### Task 1.7: Implement GrepTool cluster element
- Wrap `org.springaicommunity.agent.tools.GrepTool`
- Tools: Grep (pure Java regex-based search)
- Configure with working directory

### Task 1.8: Implement GlobTool cluster element
- Wrap `org.springaicommunity.agent.tools.GlobTool`
- Tools: Glob (file pattern matching)
- Configure with working directory

### Task 1.9: Implement SmartWebFetchTool cluster element
- Wrap `org.springaicommunity.agent.tools.SmartWebFetchTool`
- Tools: WebFetch (AI-powered web content summarization)
- Requires ChatClient for summarization - resolve from CLAUDE_CODE_MODEL
- 15-minute cache

### Task 1.10: Implement BraveWebSearchTool cluster element
- Wrap `org.springaicommunity.agent.tools.BraveWebSearchTool`
- Tools: WebSearch (domain-filtered search)
- Requires Brave API key from connection parameters

### Task 1.11: Implement SkillsTool cluster element
- Wrap `org.springaicommunity.agent.tools.SkillsTool`
- Tools: Skill (markdown-based knowledge modules)
- Skills loaded from configurable directories/resources

### Task 1.12: Implement TodoWriteTool cluster element
- Wrap `org.springaicommunity.agent.tools.TodoWriteTool`
- Tools: TodoWrite (structured task management)
- Default event handler logs at DEBUG level

### Task 1.13: Convert ClaudeCodeComponentHandler to cluster root
- Change from `@AutoService` to `@Component("claudeCode_v1_ComponentHandler")`
- Inject `ClusterElementDefinitionService`
- Register `ClaudeCodeChatAction` as action
- Register cluster elements for CLAUDE_CODE_TOOLS type
- Remove old CLI-based actions (InitializeClaude, AddMCP, Chat)
- Remove `ClaudeCodeUtil`

### Task 1.14: Implement ClaudeCodeChatAction
- Extend or follow `AbstractAiAgentChatAction` pattern
- Resolve CLAUDE_CODE_MODEL cluster element → ChatModel
- Resolve CLAUDE_CODE_TOOLS cluster elements → List<ToolCallback>
- Resolve TOOLS cluster elements → List<ToolCallback> (external)
- Create temp directory, pass to tool functions
- Build ChatClient, execute prompt
- Cleanup temp directory in finally block
- Action properties: prompt (required), systemPrompt (optional), response (optional)

### Task 1.15: Update build files
- Update `claude-code/build.gradle.kts` (remove zt-exec, add new deps)
- Create `claude-code-tool/build.gradle.kts`
- Update `settings.gradle.kts` with new submodule

### Task 1.16: Update tests
- Delete old JSON definition file
- Update `ClaudeCodeComponentHandlerTest`
- Run tests to regenerate definition JSON

---

## Phase 2: AskUserQuestionTool

### Task 2.1: Implement AskUserQuestionTool cluster element
- Wrap `org.springaicommunity.agent.tools.AskUserQuestionTool`
- Implement `QuestionHandler` for ByteChef workflow context
- Integrate with ByteChef's user notification/interaction system

### Task 2.2: Register AskUserQuestionTool
- Add to `ClaudeCodeToolComponentHandler`
- New cluster element with type CLAUDE_CODE_TOOLS

---

## Phase 3: TaskTool - Sub-agent Orchestration

### Task 3.1: Add spring-ai-agent-utils TaskTool dependency
- Multi-model routing via `ClaudeSubagentType`
- Background task execution
- Custom sub-agent definitions from Markdown

### Task 3.2: Implement TaskTool cluster element
- Configure `ClaudeSubagentType` with multiple ChatClient builders
- Register default sub-agents (general-purpose, Explore, Plan, Bash)
- Background execution via `TaskRepository`

### Task 3.3: Implement TaskOutputTool cluster element
- Non-blocking background task output retrieval
- Integrate with `TaskRepository`

---

## Phase 4: A2A Protocol Integration

### Task 4.1: Add spring-ai-agent-utils-a2a dependency
- `org.springaicommunity:spring-ai-agent-utils-a2a:0.5.0-SNAPSHOT`
- A2A Java SDK dependency

### Task 4.2: Implement A2A SubagentType integration
- Configure `A2ASubagentResolver` and `A2ASubagentExecutor`
- Register remote agent references via `SubagentReference`
- Agent card discovery via `/.well-known/agent-card.json`

### Task 4.3: Add A2A-enabled TaskTool
- Extend TaskTool with A2A subagent types
- Enable mixed local + remote agent delegation

---

## Dependencies

### External Libraries
- `org.springaicommunity:spring-ai-agent-utils:0.4.2` (Phase 1-3)
- `org.springaicommunity:spring-ai-agent-utils-a2a:0.5.0-SNAPSHOT` (Phase 4)
- Spring AI 2.0.0-M2 (already in project)

### Internal Dependencies
- `platform-component-api` (for ClaudeCodeToolFunction interface)
- `platform-workflow-worker-api` (for worker infrastructure)
- `ai:llm` module (for model utilities, optional)
- `commons-util` (for utility classes)

## Design Decisions

1. **Per-action temp directory**: Each Chat action invocation creates a fresh temp dir, cleaned up after execution. No state leakage.
2. **spring-ai-agent-utils as dependency**: Use the library directly rather than reimplementing. Reduces maintenance burden.
3. **Function-based ClusterElements**: `ClaudeCodeToolFunction` returns `List<ToolCallback>`, bridging spring-ai-agent-utils's `@Tool` pattern with ByteChef's ClusterElement system.
4. **CLAUDE_CODE_MODEL reuses ModelFunction.MODEL concept**: The model resolution follows the same pattern as AiAgent but with its own ClusterElementType name.
5. **TOOLS type shared with AiAgent**: External tools use the same `BaseToolFunction.TOOLS` type, enabling reuse of existing tool components.
