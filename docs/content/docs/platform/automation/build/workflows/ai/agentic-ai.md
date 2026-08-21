---
title: Agentic AI
description: Goal-oriented agent runs — describe the goal, and a planner picks which actions to execute and in what order.
comingSoon: true
---

# Agentic AI

The **Agentic AI** component takes a different approach from the [AI Agent](/platform/automation/build/workflows/ai/agent): instead of a model free-running with a bag of tools, you give it a **goal** and a set of candidate **actions** (cluster elements), and a goal-oriented planner (built on the Embabel Agent framework's GOAP — goal-oriented action planning) decides which actions to run and in what order to reach the goal.

## How it differs from the AI Agent

| | AI Agent | Agentic AI |
|---|---|---|
| Control flow | The LLM picks tools turn by turn during a conversation. | A planner composes a plan from the available actions up front, then executes it. |
| Best for | Conversational assistants, chat surfaces, open-ended tasks. | Deterministic goal completion inside a workflow — "get from state A to state B". |
| Building blocks | Model, memory, RAG, guardrails, tools. | A **Run** action plus **Action** cluster elements the planner can choose from, plus optional shared tools. |

## How the Planner Works

Each **Action** you attach declares an **input binding** (the named value it consumes) and an **output binding** (the named value it produces). The planner treats these bindings as a graph: it starts from the seeded input (`userGoal`, the goal you describe) and searches for a path of actions whose bindings chain together to produce the **goal output binding**. When more than one action can produce the same output binding, the planner prefers the path with the lowest total **cost**.

Because the plan is composed from bindings rather than a hand-wired sequence, you can add or remove actions without rewiring — the planner re-plans around whatever actions are available.

## Usage

Building an Agentic AI node is a three-part flow: drop the node in, attach the actions the planner may choose from, then configure the goal on the **Run** action.

1. Add the **Agentic AI** node to a workflow and open it.
2. Attach one or more **Action** cluster elements — these are the planner's vocabulary, not a linear pipeline. Configure each one's bindings, prompt, and cost (see the table below).
3. (Optional) Attach **Tools** the actions may call. Tools attached to the Agentic AI node are shared across every action; the canvas also supports per-action tools nested inside an individual Action.
4. Configure the **Run** action with the goal description and the goal output binding.

<!-- TODO screenshot: the Agentic AI node editor showing the Run action's Goal Description / Goal Output Binding / Goal Mode fields alongside two attached Action cluster elements with their input/output bindings -->

### Run action

The **Run** action defines the goal the planner works toward:

| Property | Description |
|---|---|
| **Goal Description** | Free-text description of the goal the agentic AI should achieve using the configured actions. Required. |
| **Goal Output Binding** | The output binding that, once produced, satisfies the goal. Must match the output binding of at least one configured action. Required. |
| **Goal Mode** | **Structural** (default) — the goal is satisfied as soon as the goal output binding is produced. **Smart (experimental)** — additionally asks an LLM to judge whether the produced value actually satisfies the goal description, and the planner may backtrack and try alternative paths if not. Smart mode adds an LLM call per evaluation. |
| **System Prompt** | Optional system prompt applied to the underlying model calls. |
| **Response** | Optional structured-output configuration (response format / schema), the same as the AI Agent's `chat` action. |

### Action cluster element

Each **Action** the planner may choose from carries:

| Property | Description |
|---|---|
| **Action Name** | A unique name for this action. Required. |
| **Description** | What this action does — read by the GOAP planner when composing a plan. Required. |
| **Prompt** | The prompt template for the LLM. Use `{input}` to reference the input data the action receives. Required. |
| **Input Binding** | The name of the input this action consumes from a previous action's output. Use `userGoal` for the first action that takes the user's goal directly. Required. |
| **Output Binding** | The name of the output this action produces. Must match another action's input binding or the goal's required output. Required. |
| **Cost** | GOAP edge weight for this action. When multiple actions produce the same output binding, the planner prefers the lowest-total-cost path. Raise it to discourage this action, lower it to encourage it. Defaults to `1.0`. |

At execution time the planner selects and orders the attached actions to satisfy the goal, running each one with the workflow's connections and context and passing values between them through the declared bindings.

## Agentic AI as a Tool

Like the AI Agent, the Agentic AI node also publishes itself as a callable tool, so another agent can attach a configured Agentic AI run as one of its tools — letting a conversational agent delegate a bounded, goal-driven sub-task to the planner.
