---
title: Agentic Patterns
description: Build the common agentic patterns — chaining, routing, parallelization, orchestrator-workers, and evaluator-optimizer — visually in ByteChef using flow controls and AI Agents.
---

Effective agentic systems are usually built from a small set of composable patterns rather than one monolithic "do everything" agent. ByteChef lets you build each of these patterns on the workflow canvas by combining [flow controls](/automation/build/flow-controls) with the [AI Agent](/platform/ai/agent) — no framework code required.

The five patterns below follow the taxonomy from Anthropic's *Building Effective Agents*. Each maps onto ByteChef primitives you already have.

---

## Prompt Chaining

Decompose a task into a fixed sequence of steps, where each step's output feeds the next. In ByteChef this is simply a **linear sequence of tasks** on the canvas — one or more AI Agent (or LLM) steps in order, passing data with [data pills](/automation/build/data-pills). Add a [Condition](/reference/flow-controls/condition_v1) between steps as a gate when a stage should only run if the previous output passes a check.

## Routing

Classify an input and send it down a specialized path. Use a [Condition](/reference/flow-controls/condition_v1) or [Branch](/reference/flow-controls/branch_v1) flow control — often driven by a first AI Agent step that classifies the input — to route each case to the handler best suited to it. Each branch can use a different model, prompt, or set of tools.

## Parallelization

Run independent subtasks at the same time, then combine the results. ByteChef offers several fan-out flow controls depending on the shape of the work:

- [Each](/reference/flow-controls/each_v1) / [Map](/reference/flow-controls/map_v1) — apply the same steps to every item of a list in parallel (Map collects the results).
- [Parallel](/reference/flow-controls/parallel_v1) — run several distinct task lists concurrently.
- [Fork-Join](/reference/flow-controls/fork-join_v1) — run isolated branches in parallel, each with its own context, then join.

## Orchestrator-Workers

A central agent breaks a complex job into subtasks and delegates each to a specialized worker. Build this **inside a single agent** with the **Task** tool from the [Agent Utils toolset](/platform/ai/agent/agent-utils) — the agent dynamically delegates to specialized sub-agents at runtime. Or build it **as a workflow topology** with [Subflow](/reference/flow-controls/subflow_v1), where an orchestrating workflow starts child workflows that run in isolation and report back.

## Evaluator-Optimizer

One agent produces a result; another evaluates it and feeds back improvements, looping until the result is good enough. Combine a [Loop](/reference/flow-controls/loop_v1) flow control with two AI Agent steps — a generator and an evaluator — using the evaluator's verdict as the loop's continue/break condition. For grading agent outputs systematically, see [Agent Evals](/platform/ai/agent/evals).

---

## Combining Patterns

Real systems compose these patterns: a router that dispatches to parallelized workers, an orchestrator whose workers each run their own chain, an evaluator-optimizer loop wrapped around any of the above. Because every pattern is built from the same flow controls and agent nodes, they nest and combine freely on one canvas.
