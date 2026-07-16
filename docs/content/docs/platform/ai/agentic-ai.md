---
title: Agentic AI
description: Goal-oriented agent runs — describe the goal, and a planner picks which actions to execute and in what order.
---

# Agentic AI

> **Coming soon.** Agentic AI is on the upcoming release track and is not yet available in the latest released version of ByteChef.

The **Agentic AI** component takes a different approach from the [AI Agent](/platform/ai/agent): instead of a model free-running with a bag of tools, you give it a **goal** and a set of candidate **actions** (cluster elements), and a goal-oriented planner decides which actions to run and in what order to reach the goal.

## How it differs from the AI Agent

| | AI Agent | Agentic AI |
|---|---|---|
| Control flow | The LLM picks tools turn by turn during a conversation. | A planner composes a plan from the available actions up front, then executes it. |
| Best for | Conversational assistants, chat surfaces, open-ended tasks. | Deterministic goal completion inside a workflow — "get from state A to state B". |
| Building blocks | Model, memory, RAG, guardrails, tools. | A **Run** action plus action cluster elements the planner can choose from. |

## Usage

1. Add the **Agentic AI** node to a workflow and open it.
2. Attach the **actions** the planner may use as cluster elements — these are the planner's vocabulary, not a linear pipeline.
3. Configure the **Run** action with the goal and inputs.

At execution time the planner (built on goal-oriented action planning) selects and orders actions from the attached set to satisfy the goal, executing them with the workflow's connections and context.
