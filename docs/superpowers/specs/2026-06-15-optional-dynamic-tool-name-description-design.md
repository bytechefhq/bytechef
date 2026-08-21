# Optional, Dynamically-Injected `toolName` / `toolDescription` — Design

**Date:** 2026-06-15
**Branch:** `0_732`
**Status:** Approved (pending spec review)

## Problem

Cluster-element TOOLs let an AI agent call a component action. The displayed tool
**name** and **description** (what the model sees) can be overridden by two node
parameters — `toolName` and `toolDescription`. Today only seven tools expose these
overrides, and they do so by **statically declaring** the two properties as
**required** fields inside each tool's definition:

```java
string(TOOL_NAME)
    .label("Name")
    .description("The tool name exposed to the AI model.")
    .expressionEnabled(false)
    .required(true),
string(TOOL_DESCRIPTION)
    .label("Description")
    .description("The tool description exposed to the AI model.")
    .controlType(TEXT_AREA)
    .expressionEnabled(false)
    .required(true),
```

This has two drawbacks:

1. **Every other tool cannot be renamed/redescribed.** Tools that don't declare the
   blocks give the agent no override hook at all.
2. **Boilerplate duplication.** The same two property blocks are copy-pasted across
   seven tools, and being `required(true)` they force the author to fill them even
   when the action-derived defaults are perfectly fine.

## Goal

Make `toolName` and `toolDescription` **optional override fields that every TOOL
cluster element which doesn't already declare them exposes automatically**, injected
**dynamically** by the platform. When left blank, behavior is unchanged (the existing
action-derived defaults apply). This gives authors a uniform way to override a tool's
advertised name/description without per-tool boilerplate.

**The seven tools that already declare `toolName`/`toolDescription` as `required(true)`
are left untouched** — those tools intentionally force the author to name/describe them.
The dynamic injection is purely additive and idempotent: it adds the *optional* override
fields only where they are absent, and skips any element that already declares a property
of that name. So the required-declaring tools keep their required fields; every other
TOOL element gains the optional overrides.

Non-goal: changing the **default** name/description derivation. When the override is
blank the runtime keeps deriving the name from the component+element
(`COMPONENT_ELEMENT`, e.g. `HTTPCLIENT_SENDREQUEST`) and the description from the
cluster element's own description. This change only adds the *ability to override*;
it does not alter the defaults.

## Approach

### Seam: the platform `ClusterElementDefinitionService`

Every path that surfaces a cluster-element definition to the workflow editor flows
through `ClusterElementDefinitionServiceImpl`
(`server/libs/platform/platform-component/platform-component-service`). Its methods
each construct a platform-domain `ClusterElementDefinition`
(`com.bytechef.platform.component.domain.ClusterElementDefinition`) from the SDK
definition:

- `getClusterElementDefinition(componentName, clusterElementName)`
- `getClusterElementDefinition(componentName, componentVersion, clusterElementName)`
- `getClusterElementDefinition(componentName, componentVersion, clusterElementName, clusterElementTypeName)`
- `getClusterElementDefinitions(ClusterElementType)`
- `getClusterElementDefinitions(componentName, componentVersion, ClusterElementType)`
- `getRootClusterElementDefinitions(...)` (delegates to the list method above)

These reach the editor via `ClusterElementDefinitionApiController` (REST) and
`ClusterElementDefinitionGraphQlController` (GraphQL), both of which call the service
directly. Injecting at the service therefore reaches **every** editor path.

### Injection rule

Route all the construction sites through one private helper in the service that,
**when the element's type is `TOOLS`**, prepends two **optional** properties —
`toolName` and `toolDescription` — to the definition's property list.

The injection is **idempotent**: if a property with the same name already exists on
the element, it is **not** added. This:

- prevents double-injection,
- respects any tool that still declares the fields itself, and
- automatically protects `mcp-client`, whose `McpClientCallToolAction` already owns a
  **functional** `toolName` property (selecting which MCP tool to call) — that
  property is detected and the override field is skipped, with no special-casing.

Order: `[toolName, toolDescription, ...existing properties]`, matching the current
editor layout (the override fields sit at the top, above the action's own inputs).

The injected fields mirror the existing static blocks, minus `required(true)`:

| Field             | Type   | Control    | required | expressionEnabled |
|-------------------|--------|------------|----------|-------------------|
| `toolName`        | string | (default)  | false    | false             |
| `toolDescription` | string | `TEXT_AREA`| false    | false             |

Descriptions are reworded to signal the default-on-blank behavior, e.g.
"The tool name exposed to the AI model. Defaults to the action name when left blank."

### Type detection

`ClusterElementDefinition.getType()` returns the SDK `ClusterElementType` record;
its `name()` is `"TOOLS"` for tool elements (the `BaseToolFunction.TOOLS` constant
defines `new ClusterElementType("TOOLS", "tools", "Tools", true, false)`). The helper
checks `type != null && "TOOLS".equals(type.name())`.

### Building the platform properties

The platform `Property` domain has no public builder; instances are produced by
converting an SDK property via `Property.toProperty(...)`. So the helper builds the
two override fields once using the SDK DSL —
`string("toolName").label("Name").description(...).expressionEnabled(false)` and
`string("toolDescription").label("Description").description(...).controlType(TEXT_AREA).expressionEnabled(false)`
(neither marked required) — and converts them to platform `Property` via
`Property.toProperty(...)`. These two converted platform properties are cached as
static constants in the service (they are immutable and request-independent).

### Augmenting the immutable definition

`ClusterElementDefinition` is immutable — `properties` is exposed as an unmodifiable
list and there is no setter. Add a small method to the domain class:

```java
public ClusterElementDefinition withPrependedProperties(List<? extends Property> properties)
```

It returns a **new** `ClusterElementDefinition` copying every field, with the supplied
properties prepended to the existing list. A private copy constructor backs it. The
service helper calls this only for `TOOLS` elements, passing the (idempotently
filtered) override fields.

### Constant home

The property keys (`"toolName"`, `"toolDescription"`) must match what consumption
reads. They live in the existing
`com.bytechef.platform.ai.tool.constant.ToolConstants` (`platform-ai-api`) — the
natural domain home, since `toolName`/`toolDescription` are AI-tool concepts.

`platform-component-service` (the injection site) takes a new
`implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))`
dependency and imports `ToolConstants` directly. There is exactly **one**
`ToolConstants` (no duplicate in `platform-component`).

To keep this dependency clean, `platform-ai-api`'s `api(automation-ai-mcp-api)` edge
was removed so that pulling `platform-ai-api` into a foundational platform-service
module does **not** drag the automation layer in transitively. The resulting graph
(`platform-component-service → platform-ai-api → {platform-component-api,
platform-configuration-api, platform-mcp-api}`) compiles with no project cycle.

> **Design note (revised during implementation):** an earlier draft introduced a
> second `ToolConstants` in `platform-component-api` (with `platform-ai`'s delegating
> to it) to avoid adding any module dependency. That was reverted in favor of a single
> `ToolConstants` in `platform-ai`, after the `automation-ai-mcp-api` edge was dropped
> from `platform-ai-api` to make the new dependency cost-free.

### Tools are NOT modified

The seven tools that statically declare `toolName`/`toolDescription` as `required(true)`
keep them exactly as-is:

- `HttpClientTool` (`http-client`)
- `AiAgentChatTool` (`ai/agent`)
- `ScriptPythonTool`, `ScriptJavaScriptTool`, `ScriptRubyTool` (`script`)
- `AgenticAiTool` (`ai/agenticai`)
- `WorkflowCallWorkflowTool` (`workflow`)

The injection's idempotency guard skips any element that already declares a property
named `toolName` / `toolDescription`, so these tools receive **no** injected fields and
their required overrides are preserved unchanged. (An earlier draft proposed stripping
the static blocks and relying on injection; that was rejected — the required fields are
intentional and must stay required.) No component definitions change, so no definition
snapshots need regenerating.

Also intentionally untouched — they reference `TOOL_NAME` for unrelated purposes, not
as override fields:
- `mcp-client` (`McpClientCallToolAction` / `McpClientUtils`) — `toolName` is a
  functional MCP-tool selector; idempotency skips injection there.
- `ai-agent-utils` (`AiAgentUtilsAgentClientTool`) — uses a **local**
  `TOOL_NAME = "sendTaskToRemoteAgent"` constant as an actual callback name.

### Consumption — no change needed

`AbstractToolFacade.getToolName` already falls back to the auto-generated
`COMPONENT_ELEMENT` name when the `toolName` param is absent, and
`getToolDescription` already falls back through extensions to
`clusterElementDefinition.getDescription()`. Making the fields optional simply means
the params may now be absent, which these fallbacks already handle. No consumption
code changes.

## Testing

- **Service-level (unit):** a `TOOLS` element returns with `toolName` + `toolDescription`
  prepended and `required == false`; a non-`TOOLS` element is unchanged; idempotency —
  an element that already declares a `toolName` property is not doubled.
- **Domain-level (unit):** `withPrependedProperties` returns a new instance with the
  properties prepended and all other fields copied (original unmodified).
- **Constant parity (unit):** `ToolConstants.TOOL_NAME` /
  `TOOL_DESCRIPTION` equal the new `platform-component-api` constants (guards drift).
- **Tool-level (unit):** one de-statified tool (e.g. `HttpClientTool`) exposes
  `toolName` / `toolDescription` **through the service** with `required == false`.
- **Regression:** existing tool/facade tests still pass — runtime defaults unchanged.

## Risks / Notes

- **Broader-than-"action-backed" injection.** The seam is type-driven, so *any* TOOLS
  cluster element gains the optional override fields, not only `tool(actionDefinition)`
  ones. This matches the literal request ("all tools that do not have them defined")
  and is harmless: the fields are optional and the runtime fallbacks already cover
  absent values. Idempotency prevents conflicts with elements that already own a
  matching property.
- **Constant drift** between `platform-ai` and `platform-component-api` is eliminated
  by having `ToolConstants` reference the `platform-component-api` constants (single
  source) plus a parity unit test.
