# Agentic AI Component

With the Agentic AI component, you can define a goal and let the AI autonomously plan and execute tools to achieve it using the [Embabel Agent](https://github.com/embabel/embabel-agent) framework with GOAP (Goal-Oriented Action Planning).

## Enabling the Component

The component is disabled by default. To enable it, add the `agentic` Spring profile:
`SPRING_PROFILES_ACTIVE=<your profiles>,agentic` (on the server, and in distributed deployments
also on the worker). No provider API key is required.

The `agentic` profile re-enables Embabel's `AgentPlatformAutoConfiguration`, which is excluded in
the default configuration. Without the profile the component stays invisible (its handler is
`@ConditionalOnBean(AgentPlatform.class)`) and server startup is unaffected.

**Model source**: all of the agent's LLM calls — action prompts and smart-goal evaluations —
run against the workflow's canvas-selected MODEL cluster element (e.g. OpenAI, Anthropic) with
its ByteChef connection, exactly like the AI Agent component. A model with a connection is
required on the canvas. Embabel's own model registry is never used; it is satisfied at startup
by ByteChef's inert `bytechef-canvas` placeholder model, which the profile points
`embabel.models.default-llm` at.

## Cluster Element Types

| Type | Key | Description | Multiple |
|------|-----|-------------|----------|
| MODEL | model | LLM provider (OpenAI, Claude, etc.) | Yes |
| ACTION | action | A named step with prompt, input/output bindings, and tools | Yes |
| TOOLS | tools | Tools available to actions | Yes |
| GOAL | goal | The goal the agent must achieve | No |

## GOAP Planning Example

Given 3 user-configured action steps, the GOAP planner sees:

```
Blackboard: {GoalInput("Create market report")}

Available actions:
  research:  GoalInput → Step1Output     (has search tools)
  analyze:   Step1Output → Step2Output   (has calculator tools)
  write:     Step2Output → Step3Output   (no extra tools)

Goal: satisfiedBy = Step3Output

GOAP planning (backward chaining via A*):
  Need: Step3Output
  → "write" produces Step3Output, needs Step2Output
  → "analyze" produces Step2Output, needs Step1Output
  → "research" produces Step1Output, needs GoalInput ✓ (on Blackboard)

Plan: research → analyze → write
```

Each action step produces a unique JVM type (`Step1Output`, `Step2Output`, etc.) so the planner:
- **Cannot skip steps** — "write" needs `Step2Output` which only "analyze" produces
- **Cannot reorder** — `Step2Output` depends on `Step1Output`
- **Replans after each step** — if "research" fails, it can try alternative paths
- **Only completes** when the final step's output type appears on the Blackboard

## ACTION Cluster Element Properties

| Property | Description | Required |
|----------|-------------|----------|
| actionName | A unique name for this action | Yes |
| actionDescription | What this action does (used by the GOAP planner) | Yes |
| actionPrompt | Prompt template for the LLM. Use `{input}` to reference input data | Yes |
| inputBinding | Name of the input this action needs (use `userGoal` for the first action) | Yes |
| outputBinding | Name of the output this action produces | Yes |
| actionCost | GOAP edge weight; lower-cost paths win when alternatives exist (default 1.0) | No |
| outputSchema | Structured output properties (name, type, description) — makes the binding *typed* | No |

## Typed Bindings (Structured Output)

By default a binding carries free text. Declaring an `outputSchema` on an action turns its output
binding into a **typed binding**, backed by an Embabel `DynamicType` (Embabel 1.0's runtime-declared
domain model — no JVM class needed):

- The model is instructed to return ONLY a JSON object with the declared properties; the value
  travels the blackboard as a `_typeName`-tagged map whose type name is derived from the binding
  name (`marketAnalysis` → `MarketAnalysis`).
- Downstream actions consuming a typed binding receive the object rendered as JSON in `{input}`.
- If the **goal output binding** is typed, the run action returns the structured object instead of
  a string — downstream workflow tasks can reference individual properties.
- All actions producing the same output binding must declare the same schema (or none): type
  matching on the blackboard is strict, so alternative producers must be interchangeable for their
  consumers. Mismatches are rejected at validation time with an actionable message.

Example `outputSchema` on an action:

```json
"outputSchema": [
  { "name": "summary", "type": "string", "description": "One-paragraph market summary" },
  { "name": "keyTrends", "type": "array", "description": "The most important trends" },
  { "name": "confidence", "type": "number", "description": "Confidence score from 0 to 1" }
]
```

## Example Configuration

```json
{
  "type": "agenticAi/v1/run",
  "parameters": {
    "goalDescription": "Create a comprehensive market analysis report for electric vehicles"
  },
  "extensions": {
    "clusterElements": {
      "model": {
        "type": "openAi/v1/model",
        "parameters": { "model": "gpt-4o" }
      },
      "action": [
        {
          "type": "agenticAi/v1/action",
          "parameters": {
            "actionName": "research",
            "actionDescription": "Research the topic using web search tools",
            "actionPrompt": "Research the following topic thoroughly using available tools: {input}",
            "inputBinding": "userGoal",
            "outputBinding": "researchData"
          }
        },
        {
          "type": "agenticAi/v1/action",
          "parameters": {
            "actionName": "analyze",
            "actionDescription": "Analyze the research data and extract key insights",
            "actionPrompt": "Analyze the following research data and extract key market insights, trends, and statistics: {input}",
            "inputBinding": "researchData",
            "outputBinding": "analysis"
          }
        },
        {
          "type": "agenticAi/v1/action",
          "parameters": {
            "actionName": "write",
            "actionDescription": "Write the final market analysis report",
            "actionPrompt": "Write a comprehensive market analysis report based on the following analysis: {input}",
            "inputBinding": "analysis",
            "outputBinding": "finalReport"
          }
        }
      ],
      "tools": [
        {
          "type": "googleSearch/v1/search",
          "parameters": { "toolName": "web_search", "toolDescription": "Search the web" }
        },
        {
          "type": "calculator/v1/calculate",
          "parameters": { "toolName": "calculator", "toolDescription": "Perform calculations" }
        }
      ]
    }
  }
}
```
