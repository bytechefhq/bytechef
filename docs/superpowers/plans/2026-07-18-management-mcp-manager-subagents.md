# Plan: expose AI Hub manager subagents on the management MCP server

Spec: `docs/superpowers/specs/2026-07-18-management-mcp-manager-subagents-design.md`

1. **`WorkspaceScopedManagerToolCallback`** (`ai-hub-service`, `tool` package)
   - Wraps a `ManagerSubAgentToolCallback`; same tool name, description extended with the
     workspaceId contract, input schema `{request, workspaceId?}`.
   - `call()`: resolve workspaceId (explicit input → single-workspace default → typed
     error listing candidates), merge it into the forwarded `ToolContext` under
     `AiHubToolInvocationContext.TOOL_CONTEXT_WORKSPACE_ID_KEY`, delegate with a
     `{request}` payload.

2. **`AiHubManagerMcpContributorConfiguration`** (`ai-hub-service`, `config` package)
   - `@ConditionalOnProperty(bytechef.ai.hub.enabled)`.
   - One `McpServerToolCallbackContributor` bean collecting the four manager ChatClient
     `ObjectProvider`s (mcp/personal-agent/deployment/api-collection); each available
     client is turned into its delegate via the existing `create*ToolCallback` factory
     and wrapped in `WorkspaceScopedManagerToolCallback`.

3. **Tests**
   - Wrapper: explicit workspaceId injected into forwarded context; single workspace
     auto-selected; multiple workspaces produce an error listing ids/names; blank
     request rejected.
   - Contributor: only present ChatClients yield tools; tool names match the subagent
     keys.

4. **Verification**: javac parse check + symbol greps (Gradle 9 unavailable in this
   session); CI runs the real compile.
