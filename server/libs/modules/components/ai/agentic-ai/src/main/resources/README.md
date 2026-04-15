# Agentic AI Component

With the Agentic AI component, you can define a goal and let the AI autonomously plan and execute tools to achieve it using the [Embabel Agent](https://github.com/embabel/embabel-agent) framework with GOAP (Goal-Oriented Action Planning).

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
