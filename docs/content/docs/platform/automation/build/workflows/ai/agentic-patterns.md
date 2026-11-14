---
title: Agentic Patterns
description: Build the common agentic patterns - chaining, routing, parallelization, orchestrator-workers, and evaluator-optimizer - visually in ByteChef using flow controls and AI Agents.
---

Agentic systems are usually composed from a small set of patterns instead of one agent that does everything. You build each pattern on the workflow canvas by combining [flow controls](/platform/automation/build/workflows/flow-controls) with the [AI Agent](/platform/automation/build/workflows/ai/agent). No framework code is required.

The five patterns below follow the taxonomy from Anthropic's *Building Effective Agents*. Each section names the ByteChef building blocks for the pattern.

## Prompt Chaining

Split a task into a fixed sequence of steps where each step's output is the next step's input. Build it as a linear sequence of tasks on the canvas: AI Agent (or LLM) steps in order, passing data with [data pills](/platform/automation/build/workflows/data-pills). Add a [Condition](/reference/flow-controls/condition_v1) between steps as a gate when a stage should only run if the previous output passes a check.

## Routing

Classify an input and send it to a specialized path. Use a [Condition](/reference/flow-controls/condition_v1) or [Branch](/reference/flow-controls/branch_v1) flow control, typically after an AI Agent step that classifies the input, to send each case to its handler. Each branch can use a different model, prompt, or set of tools.

## Parallelization

Run independent subtasks at the same time, then combine the results. Choose the flow control by the shape of the work:

- [Each](/reference/flow-controls/each_v1) / [Map](/reference/flow-controls/map_v1) - apply the same steps to every item of a list in parallel. Map collects the results.
- [Parallel](/reference/flow-controls/parallel_v1) - run several distinct task lists concurrently.
- [Fork-Join](/reference/flow-controls/fork-join_v1) - run isolated branches in parallel, each with its own context, then join.

## Orchestrator-Workers

A central agent splits a job into subtasks and delegates each one to a specialized worker. There are two ways to build it:

- **Inside one agent** - attach the **Task** tool from the [Agent Utils toolset](/platform/automation/build/workflows/ai/agent/agent-utils) and define subagents. The agent delegates to them at run time.
- **As a workflow topology** - use [Subflow](/reference/flow-controls/subflow_v1). An orchestrating workflow starts child workflows that run in isolation and return their results.

## Evaluator-Optimizer

One agent produces a result and another evaluates it and returns feedback, in a loop until the result passes. Use a [Loop](/reference/flow-controls/loop_v1) flow control with **Loop Forever** turned on and two AI Agent steps (a generator and an evaluator). Add a [Condition](/reference/flow-controls/condition_v1) on the evaluator's verdict that runs [Loop Break](/reference/flow-controls/loop-break_v1) when the result passes. For grading agent outputs systematically, see [Agent Evals](/platform/automation/build/workflows/ai/agent/evals).

## Combining Patterns

The patterns combine: a router that sends work to parallel workers, an orchestrator whose workers each run a chain, or an evaluator-optimizer loop around any of these. All patterns use the same flow controls and agent nodes, so they nest on one canvas.
