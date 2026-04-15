/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.ai.agenticai.embabel

import com.embabel.agent.api.dsl.agent
import com.embabel.agent.api.tool.Tool
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.Budget
import com.embabel.agent.core.IoBinding
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.support.LlmCall
import com.embabel.agent.experimental.primitive.PromptCondition
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import org.slf4j.LoggerFactory
import org.springframework.ai.tool.ToolCallback

private val logger = LoggerFactory.getLogger(EmbabelAgentRunner::class.java)

/**
 * Describes a single GOAP action the planner may choose from.
 *
 * The planner uses [inputBinding] as the action's precondition (a value must exist under that name
 * on the blackboard) and [outputBinding] as its effect (after running, a value is written under
 * that name). Action list order is irrelevant — the planner decides sequencing, may skip actions
 * whose effects are unneeded, and may pick between alternatives when multiple actions produce the
 * same output binding.
 *
 * [cost] is the GOAP edge weight for this action. When several actions produce the same
 * [outputBinding], the planner prefers the path with the lowest total cost, so costs are the knob
 * users have for nudging the planner toward preferred alternatives (e.g., cheap/fast action vs.
 * expensive/high-quality action). A cost of `1.0` is a reasonable default; use higher values to
 * discourage an action and lower values to encourage it.
 */
@SuppressFBWarnings("EI")
data class ActionStep(
    val name: String,
    val description: String,
    val prompt: String,
    val inputBinding: String,
    val outputBinding: String,
    val toolCallbacks: List<ToolCallback>,
    val cost: Double = DEFAULT_ACTION_COST,
) {
    companion object {
        /** Default per-action cost when the user does not specify one. */
        const val DEFAULT_ACTION_COST: Double = 1.0
    }
}

/**
 * Single carrier type for values flowing between actions on the blackboard.
 *
 * Distinctness between bindings comes from the *name* half of [IoBinding] (`name:type`), not from
 * JVM type identity — so every action reads and writes [Binding], and the planner discriminates by
 * the binding name the user configured on canvas.
 */
data class Binding(val content: String)

/**
 * Bridges ByteChef's canvas-authored agentic actions with Embabel's GOAP planner.
 *
 * Each [ActionStep] becomes an Embabel prompted-transformer action whose precondition and effect
 * are named blackboard slots (`IoBinding` of `name:Binding`). The planner is given:
 *   - a seed binding named [USER_GOAL_BINDING] containing the user's goal description,
 *   - a goal satisfied by the presence of a binding named `goalOutputBinding`,
 *   - the full set of user-configured actions (order-independent).
 *
 * From that it selects a valid sequence of actions (or reports the goal is unreachable). When
 * multiple configured actions produce the same binding, the planner picks between them by
 * minimizing total plan cost (see [ActionStep.cost]) — so branching is exercised in structural mode
 * too, not only in smart-goal mode.
 *
 * **Smart goal mode** (opt-in via `smartGoal = true`): the structural `goalOutputBinding`
 * requirement is kept as the planner's target, and additionally a [PromptCondition] is attached to
 * the goal's preconditions. After the binding is produced, the LLM is asked whether its content
 * actually satisfies `goalDescription`. If not, the planner may backtrack and try an alternative
 * action path that also produces `goalOutputBinding` (the canvas may declare several). This adds an
 * LLM call per goal evaluation, so it is disabled by default. Uses the experimental
 * [PromptCondition] API; revisit when Embabel promotes it out of `experimental.primitive`.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION")
class EmbabelAgentRunner(private val agentPlatform: AgentPlatform) {

    fun run(
        actionSteps: List<ActionStep>,
        goalDescription: String,
        goalOutputBinding: String,
        smartGoal: Boolean,
        systemPrompt: String?,
    ): String {
        require(actionSteps.isNotEmpty()) { "At least one action step is required" }
        require(goalOutputBinding.isNotBlank()) { "goalOutputBinding must not be blank" }
        require(actionSteps.any { it.outputBinding == goalOutputBinding }) {
            "No configured action produces the goal output binding '$goalOutputBinding'"
        }

        // Without at least one action reading from the seeded USER_GOAL_BINDING slot, the planner
        // has no entry point: every action's precondition would depend on an output that nothing
        // produces, and Embabel would report the goal unreachable at runtime. Fail up front with a
        // clear message so canvas misconfiguration surfaces at validation time instead.
        require(actionSteps.any { it.inputBinding == USER_GOAL_BINDING }) {
            "At least one action must use '$USER_GOAL_BINDING' as its inputBinding to serve as an entry point"
        }

        // Embabel identifies actions by name within an agent; duplicates would silently shadow each
        // other and destroy the planner's ability to choose between alternatives that produce the
        // same output binding. Reject up front with a clear message.
        val duplicateActionNames = actionSteps.groupingBy { it.name }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        require(duplicateActionNames.isEmpty()) {
            "Duplicate action names are not allowed: $duplicateActionNames"
        }

        val goalConditions = if (smartGoal) {
            listOf(buildSmartGoalCondition(goalDescription, goalOutputBinding))
        } else {
            emptyList()
        }

        val embabelAgent = agent(
            name = "bytechef-agentic-ai",
            description = goalDescription,
        ) {
            for (actionStep in actionSteps) {
                val stepTools = actionStep.toolCallbacks.map { toEmbabelTool(it) }

                val stepCost = actionStep.cost

                promptedTransformer<Binding, Binding>(
                    name = actionStep.name,
                    description = actionStep.description,
                    inputVarName = actionStep.inputBinding,
                    outputVarName = actionStep.outputBinding,
                    cost = { _ -> stepCost },
                    tools = stepTools,
                ) { context -> buildPrompt(actionStep, context.input.content, systemPrompt) }
            }

            goal(
                name = "achieve-goal",
                description = goalDescription,
                inputs = setOf(IoBinding(name = goalOutputBinding, type = Binding::class)),
                pre = goalConditions,
            )
        }

        // runAgentFrom accepts the agent directly (see AgentPlatform.runAgentFrom docs);
        // deploying is only required when other agents or the platform itself need to discover
        // this agent by name. For one-shot canvas runs we skip deploy to avoid accumulating
        // identically-named agents in the platform's registry.
        val agentProcess = agentPlatform.runAgentFrom(
            embabelAgent,
            buildProcessOptions(smartGoal),
            mapOf(USER_GOAL_BINDING to Binding(goalDescription)),
        )

        val produced = agentProcess[goalOutputBinding]

        when {
            produced == null -> throw AgenticAiGoalNotAchievedException(
                "Agentic AI plan finished without producing a value at goal binding '$goalOutputBinding'. " +
                    "The planner may have exhausted its budget, failed smart-goal evaluation, or found the " +
                    "goal unreachable from the configured actions."
            )
            produced !is Binding -> throw AgenticAiGoalNotAchievedException(
                "Agentic AI plan wrote an unexpected type (${produced.javaClass.name}) at goal binding " +
                    "'$goalOutputBinding'; expected ${Binding::class.java.name}."
            )
            produced.content.isEmpty() -> throw AgenticAiGoalNotAchievedException(
                "Agentic AI plan produced an empty value at goal binding '$goalOutputBinding'."
            )
            else -> return produced.content
        }
    }

    /**
     * Builds [ProcessOptions] with an explicit [Budget] so the planner's early-termination policy
     * is pinned to ByteChef-controlled values instead of Embabel's library defaults (which may
     * change across versions).
     *
     * Smart-goal mode can backtrack through alternative action paths when the LLM rejects a
     * produced value, so we grant it a higher action cap; structural mode runs a deterministic
     * plan and gets the tighter cap.
     */
    private fun buildProcessOptions(smartGoal: Boolean): ProcessOptions {
        val budget = if (smartGoal) {
            Budget(cost = SMART_GOAL_COST_LIMIT, actions = SMART_GOAL_ACTION_LIMIT, tokens = TOKEN_LIMIT)
        } else {
            Budget(cost = COST_LIMIT, actions = ACTION_LIMIT, tokens = TOKEN_LIMIT)
        }

        return ProcessOptions(budget = budget)
    }

    private fun toEmbabelTool(toolCallback: ToolCallback): Tool {
        val toolName = toolCallback.toolDefinition.name()

        return Tool.create(
            toolName,
            toolCallback.toolDefinition.description(),
        ) { toolInput ->
            try {
                Tool.Result.text(toolCallback.call(toolInput))
            } catch (e: Exception) {
                // Tool failures must not be swallowed into the generic "goal not achieved" error:
                // log the real cause with the tool name so operators can diagnose, then surface the
                // exception as a tool-level error the planner can see instead of a silent empty
                // result that would mask the failure as a budget/unreachable problem.
                logger.error("Tool '{}' invocation failed: {}", toolName, e.message, e)
                throw e
            }
        }
    }

    companion object {
        /**
         * Seed binding name for the user's goal description. Actions that should be considered as
         * entry points set their `inputBinding` to this value.
         */
        const val USER_GOAL_BINDING: String = "userGoal"

        /** Max actions a structural-goal plan may execute before early termination. */
        private const val ACTION_LIMIT: Int = 50

        /** Max actions a smart-goal plan may execute; higher to accommodate LLM-driven backtracking. */
        private const val SMART_GOAL_ACTION_LIMIT: Int = 75

        /** Max tokens any plan may consume before early termination. */
        private const val TOKEN_LIMIT: Int = 1_000_000

        /** Cost ceiling (USD) for structural-goal plans. */
        private const val COST_LIMIT: Double = 2.0

        /** Cost ceiling (USD) for smart-goal plans; higher to accommodate extra goal-evaluation LLM calls. */
        private const val SMART_GOAL_COST_LIMIT: Double = 3.0
    }
}

/**
 * Merges the user-authored action prompt with the blackboard input and the optional system prompt.
 *
 * If the prompt contains the `{input}` placeholder, every occurrence is replaced with
 * [inputContent] (literal replacement — no regex interpretation, so `$` / `\` in the input are
 * safe). If the prompt contains no placeholder, the input is still delivered by appending a clearly
 * labeled `Input:` section, so the action never silently loses the upstream binding.
 */
private const val INPUT_PLACEHOLDER = "{input}"

private fun buildPrompt(actionStep: ActionStep, inputContent: String, systemPrompt: String?): String {
    val userPrompt = actionStep.prompt

    val basePrompt = if (userPrompt.contains(INPUT_PLACEHOLDER)) {
        userPrompt.replace(INPUT_PLACEHOLDER, inputContent)
    } else {
        buildString {
            append(userPrompt)
            append("\n\nInput:\n")
            append(inputContent)
        }
    }

    return buildString {
        append(basePrompt)

        if (!systemPrompt.isNullOrBlank()) {
            append("\n\nAdditional context:\n")
            append(systemPrompt)
        }
    }
}

/**
 * Builds an LLM-evaluated condition that asks whether the value currently bound to
 * [goalOutputBinding] semantically satisfies [goalDescription]. Uses the platform's default LLM
 * (the same one the action transformers resolve). The condition is named so log output and
 * Embabel's condition-caching can identify it; the name is stable across runs of the same agent
 * so the planner's condition memoization applies.
 */
private fun buildSmartGoalCondition(goalDescription: String, goalOutputBinding: String): PromptCondition {
    return PromptCondition(
        name = "smart-goal-$goalOutputBinding",
        prompt = { context ->
            val producedValue = when (val bound = context.processContext.agentProcess[goalOutputBinding]) {
                is Binding -> bound.content
                null -> "(nothing produced yet)"
                else -> {
                    logger.warn(
                        "Smart-goal condition for binding '{}' encountered unexpected bound type {}; " +
                            "falling back to toString(). This usually means an action wrote a value of the " +
                            "wrong type at the goal binding.",
                        goalOutputBinding, bound.javaClass.name,
                    )
                    bound.toString()
                }
            }

            """
            Goal: $goalDescription

            Current value at binding "$goalOutputBinding":
            $producedValue

            Does the current value satisfy the goal? Answer true only if the value directly and
            completely addresses the goal; answer false if it is missing information, off-topic, or
            only partially addresses the goal.
            """.trimIndent()
        },
        llm = LlmCall(),
    )
}
