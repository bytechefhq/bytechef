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

import com.embabel.agent.api.common.OperationContext
import com.embabel.agent.api.common.TransformationActionContext
import com.embabel.agent.api.dsl.agent
import com.embabel.agent.core.ActionRunner
import com.embabel.agent.core.ActionStatus
import com.embabel.agent.core.AgentPlatform
import com.embabel.agent.core.Budget
import com.embabel.agent.core.Cardinality
import com.embabel.agent.core.Condition
import com.embabel.agent.core.DomainType
import com.embabel.agent.core.DynamicType
import com.embabel.agent.core.IoBinding
import com.embabel.agent.core.ProcessContext
import com.embabel.agent.core.ProcessOptions
import com.embabel.agent.core.TYPE_LABELS_KEY
import com.embabel.agent.core.TYPE_NAME_KEY
import com.embabel.agent.core.ValuePropertyDefinition
import com.embabel.agent.core.support.AbstractAction
import com.embabel.common.core.types.ZeroToOne
import com.bytechef.platform.ai.util.TokenUsageHolder
import com.embabel.plan.common.condition.ConditionDetermination
import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.ChatResponse
import org.springframework.ai.tool.ToolCallback

private val logger = LoggerFactory.getLogger(EmbabelAgentRunner::class.java)

private val objectMapper = ObjectMapper()

data class OutputProperty(
    val name: String,
    val type: String = "string",
    val description: String? = null,
)

@SuppressFBWarnings("EI")
data class ActionStep @JvmOverloads constructor(
    val name: String,
    val description: String,
    val prompt: String,
    val inputBinding: String,
    val outputBinding: String,
    val toolCallbacks: List<ToolCallback>,
    val cost: Double = DEFAULT_ACTION_COST,
    val outputProperties: List<OutputProperty> = emptyList(),
) {
    companion object {
        const val DEFAULT_ACTION_COST: Double = 1.0
    }
}

data class Binding(val content: String)

fun interface ActionCompletionListener {

    fun onActionCompleted(bindingName: String, value: Any)
}

internal class TokenUsageAccumulator {

    private val promptTokens = AtomicInteger()
    private val completionTokens = AtomicInteger()

    fun record(chatResponse: ChatResponse?) {
        val metadata = chatResponse?.metadata ?: return
        val usage = metadata.usage ?: return

        promptTokens.addAndGet(usage.promptTokens?.toInt() ?: 0)
        completionTokens.addAndGet(usage.completionTokens?.toInt() ?: 0)
    }

    fun flushToHolder() {
        val totalPromptTokens = promptTokens.get()
        val totalCompletionTokens = completionTokens.get()

        if (totalPromptTokens == 0 && totalCompletionTokens == 0) {
            return
        }

        TokenUsageHolder.capture(totalPromptTokens, totalCompletionTokens)
    }
}

internal class DynamicTransformationAction(
    name: String,
    description: String,
    actionCost: Double,
    private val inputVarName: String,
    private val inputTypeName: String,
    private val outputVarName: String,
    outputTypeName: String,
    private val declaredDomainTypes: Collection<DomainType>,
    private val inputPropertyNames: Set<String>,
    private val actionCompletionListener: ActionCompletionListener?,
    private val block: (TransformationActionContext<Any, Any>) -> Any,
) : AbstractAction(
    name = name,
    description = description,
    cost = { _ -> actionCost },
    inputs = setOf(IoBinding(inputVarName, inputTypeName)),
    outputs = setOf(IoBinding(outputVarName, outputTypeName)),
    toolGroups = emptySet(),
    canRerun = false,
) {

    override val domainTypes: Collection<DomainType>
        get() = declaredDomainTypes

    override fun execute(processContext: ProcessContext): ActionStatus = ActionRunner.execute(processContext) {
        val input = processContext.agentProcess.getValue(inputVarName, inputTypeName)
            ?: error(
                "Action '$name' found no value of type '$inputTypeName' at binding '$inputVarName'; " +
                    "an upstream action produced a value of a different type than this action expects."
            )

        val output = block(
            TransformationActionContext(
                input = input,
                processContext = processContext,
                action = this,
                inputClass = Any::class.java,
                outputClass = Any::class.java,
            )
        )

        processContext.blackboard[outputVarName] = output

        if (actionCompletionListener != null) {
            try {
                actionCompletionListener.onActionCompleted(outputVarName, output)
            } catch (e: RuntimeException) {
                logger.warn("Failed to checkpoint binding '{}' after action '{}'", outputVarName, name, e)
            }
        }
    }

    override fun referencedInputProperties(variable: String): Set<String> = inputPropertyNames

    override fun toString() = "${javaClass.simpleName}: name=$name"
}

internal class CanvasSmartGoalCondition(
    private val goalDescription: String,
    private val goalOutputBinding: String,
    private val chatModel: ChatModel,
    private val tokenUsageAccumulator: TokenUsageAccumulator? = null,
) : Condition {

    override val name: String = "smart-goal-$goalOutputBinding"

    override val cost: ZeroToOne = 1.0

    override fun evaluate(context: OperationContext): ConditionDetermination {
        val producedValue = when (val bound = context.processContext.agentProcess[goalOutputBinding]) {
            is Binding -> bound.content
            is Map<*, *> -> renderInputContent(bound)
            null -> return ConditionDetermination.FALSE
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

        val prompt =
            """
            Goal: $goalDescription

            Current value at binding "$goalOutputBinding":
            $producedValue

            Does the current value satisfy the goal? Answer with EXACTLY one word: true or false.
            Answer true only if the value directly and completely addresses the goal; answer false
            if it is missing information, off-topic, or only partially addresses the goal.
            """.trimIndent()

        val chatResponse = ChatClient.create(chatModel)
            .prompt()
            .user(prompt)
            .call()
            .chatResponse()

        tokenUsageAccumulator?.record(chatResponse)

        val answer = chatResponse?.result
            ?.output
            ?.text
            ?.trim()
            ?.lowercase()
            ?: ""

        return when {
            answer.startsWith("true") -> ConditionDetermination.TRUE
            answer.startsWith("false") -> ConditionDetermination.FALSE
            else -> {
                logger.warn(
                    "Smart-goal condition '{}' got an unparseable answer '{}'; treating the goal as " +
                        "not yet satisfied.",
                    name, answer.take(80),
                )
                ConditionDetermination.FALSE
            }
        }
    }
}

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("BC_BAD_CAST_TO_ABSTRACT_COLLECTION")
class EmbabelAgentRunner(private val agentPlatform: AgentPlatform) {

    @JvmOverloads
    fun run(
        actionSteps: List<ActionStep>,
        goalDescription: String,
        goalOutputBinding: String,
        smartGoal: Boolean,
        systemPrompt: String?,
        chatModel: ChatModel,
        seedBindings: Map<String, Any> = emptyMap(),
        actionCompletionListener: ActionCompletionListener? = null,
    ): Any {
        require(actionSteps.isNotEmpty()) { "At least one action step is required" }
        require(goalOutputBinding.isNotBlank()) { "goalOutputBinding must not be blank" }
        require(actionSteps.any { it.outputBinding == goalOutputBinding }) {
            "No configured action produces the goal output binding '$goalOutputBinding'"
        }

        require(actionSteps.any { it.inputBinding == USER_GOAL_BINDING }) {
            "At least one action must use '$USER_GOAL_BINDING' as its inputBinding to serve as an entry point"
        }

        val duplicateActionNames = actionSteps.groupingBy { it.name }
            .eachCount()
            .filterValues { it > 1 }
            .keys

        require(duplicateActionNames.isEmpty()) {
            "Duplicate action names are not allowed: $duplicateActionNames"
        }

        val bindingNamesWithColon = actionSteps.flatMap { listOf(it.inputBinding, it.outputBinding) }
            .plus(goalOutputBinding)
            .filter { it.contains(":") }
            .distinct()

        require(bindingNamesWithColon.isEmpty()) {
            "Binding names must not contain ':' (reserved as Embabel's name:type separator): $bindingNamesWithColon"
        }

        val bindingTypes = resolveBindingTypes(actionSteps)

        val tokenUsageAccumulator = TokenUsageAccumulator()

        val goalConditions = if (smartGoal) {
            listOf(CanvasSmartGoalCondition(goalDescription, goalOutputBinding, chatModel, tokenUsageAccumulator))
        } else {
            emptyList()
        }

        val embabelAgent = agent(
            name = "bytechef-agentic-ai",
            description = goalDescription,
        ) {
            for (actionStep in actionSteps) {
                val inputType = bindingTypes[actionStep.inputBinding]
                val outputType = bindingTypes[actionStep.outputBinding]

                action {
                    buildDynamicAction(
                        actionStep, inputType, outputType, systemPrompt, chatModel, tokenUsageAccumulator,
                        actionCompletionListener)
                }
            }

            goal(
                name = "achieve-goal",
                description = goalDescription,
                inputs = setOf(
                    IoBinding(
                        name = goalOutputBinding,
                        type = bindingTypes[goalOutputBinding]?.name ?: Binding::class.java.name,
                    )
                ),
                pre = goalConditions,
            )
        }

        val initialBindings = LinkedHashMap<String, Any>()

        initialBindings[USER_GOAL_BINDING] = Binding(goalDescription)

        seedBindings.forEach { (bindingName, value) ->
            if (bindingName != USER_GOAL_BINDING) {
                initialBindings[bindingName] = if (value is String) Binding(value) else value
            }
        }

        try {
            val agentProcess = agentPlatform.runAgentFrom(
                embabelAgent,
                buildProcessOptions(smartGoal),
                initialBindings,
            )

            return extractGoalResult(agentProcess[goalOutputBinding], goalOutputBinding)
        } finally {
            tokenUsageAccumulator.flushToHolder()
        }
    }

    private fun resolveBindingTypes(actionSteps: List<ActionStep>): Map<String, DynamicType> {
        val bindingTypes = mutableMapOf<String, DynamicType>()

        for ((bindingName, producers) in actionSteps.groupBy { it.outputBinding }) {
            val typedProducers = producers.filter { producer -> producer.outputProperties.isNotEmpty() }

            if (typedProducers.isEmpty()) {
                continue
            }

            require(bindingName != USER_GOAL_BINDING) {
                "Actions producing '$USER_GOAL_BINDING' must not declare an output schema: that binding is " +
                    "seeded with the plain-text goal description"
            }
            require(typedProducers.size == producers.size) {
                "All actions producing binding '$bindingName' must agree on its output schema, but only " +
                    "${typedProducers.size} of ${producers.size} declare one " +
                    "(producers: ${producers.map { it.name }})"
            }

            val distinctPropertyNameLists = producers
                .map { producer -> producer.outputProperties.map { it.name } }
                .distinct()

            require(distinctPropertyNameLists.size == 1) {
                "Actions producing binding '$bindingName' declare different output schemas: " +
                    "$distinctPropertyNameLists. Alternative producers of one binding must produce the same " +
                    "shape so downstream actions can consume any of them."
            }

            val outputProperties = producers.first().outputProperties

            require(outputProperties.none { it.name.isBlank() }) {
                "Binding '$bindingName' has an output schema property with a blank name"
            }

            val duplicatePropertyNames = outputProperties.groupingBy { it.name }
                .eachCount()
                .filterValues { it > 1 }
                .keys

            require(duplicatePropertyNames.isEmpty()) {
                "Binding '$bindingName' declares duplicate output schema properties: $duplicatePropertyNames"
            }

            bindingTypes[bindingName] = DynamicType(
                name = dynamicTypeNameFor(bindingName),
                description = "Structured value of the '$bindingName' binding",
                ownProperties = outputProperties.map { outputProperty ->
                    ValuePropertyDefinition(
                        name = outputProperty.name,
                        type = outputProperty.type,
                        cardinality = Cardinality.ONE,
                        description = outputProperty.description ?: outputProperty.name,
                    )
                },
            )
        }

        return bindingTypes
    }

    private fun buildDynamicAction(
        actionStep: ActionStep,
        inputType: DynamicType?,
        outputType: DynamicType?,
        systemPrompt: String?,
        chatModel: ChatModel,
        tokenUsageAccumulator: TokenUsageAccumulator,
        actionCompletionListener: ActionCompletionListener?,
    ): DynamicTransformationAction {
        val bindingTypeName = Binding::class.java.name

        return DynamicTransformationAction(
            name = actionStep.name,
            description = actionStep.description,
            actionCost = actionStep.cost,
            inputVarName = actionStep.inputBinding,
            inputTypeName = inputType?.name ?: bindingTypeName,
            outputVarName = actionStep.outputBinding,
            outputTypeName = outputType?.name ?: bindingTypeName,
            declaredDomainTypes = listOf(
                inputType ?: DynamicType(bindingTypeName),
                outputType ?: DynamicType(bindingTypeName),
            ),
            inputPropertyNames = inputType?.ownProperties
                ?.map { it.name }
                ?.toSet()
                ?: emptySet(),
            actionCompletionListener = actionCompletionListener,
        ) { context ->
            val prompt = buildPrompt(actionStep, renderInputContent(context.input), systemPrompt) +
                typedOutputInstructions(outputType)

            val responseText = callCanvasModel(chatModel, prompt, actionStep.toolCallbacks, tokenUsageAccumulator)

            if (outputType == null) {
                Binding(responseText)
            } else {
                parseTypedOutput(responseText, outputType, actionStep.name)
            }
        }
    }

    private fun extractGoalResult(produced: Any?, goalOutputBinding: String): Any {
        return when {
            produced == null -> throw AgenticAiGoalNotAchievedException(
                "Agentic AI plan finished without producing a value at goal binding '$goalOutputBinding'. " +
                    "The planner may have exhausted its budget, failed smart-goal evaluation, or found the " +
                    "goal unreachable from the configured actions."
            )
            produced is Binding -> {
                if (produced.content.isEmpty()) {
                    throw AgenticAiGoalNotAchievedException(
                        "Agentic AI plan produced an empty value at goal binding '$goalOutputBinding'."
                    )
                }

                produced.content
            }
            produced is Map<*, *> -> {
                val structuredResult = LinkedHashMap<String, Any?>()

                produced.forEach { (key, value) ->
                    if (key != TYPE_NAME_KEY && key != TYPE_LABELS_KEY) {
                        structuredResult[key.toString()] = value
                    }
                }

                if (structuredResult.isEmpty()) {
                    throw AgenticAiGoalNotAchievedException(
                        "Agentic AI plan produced an empty object at goal binding '$goalOutputBinding'."
                    )
                }

                structuredResult
            }
            else -> throw AgenticAiGoalNotAchievedException(
                "Agentic AI plan wrote an unexpected type (${produced.javaClass.name}) at goal binding " +
                    "'$goalOutputBinding'; expected ${Binding::class.java.name} or a type-tagged map."
            )
        }
    }

    private fun buildProcessOptions(smartGoal: Boolean): ProcessOptions {
        val budget = if (smartGoal) {
            Budget(cost = SMART_GOAL_COST_LIMIT, actions = SMART_GOAL_ACTION_LIMIT, tokens = TOKEN_LIMIT)
        } else {
            Budget(cost = COST_LIMIT, actions = ACTION_LIMIT, tokens = TOKEN_LIMIT)
        }

        return ProcessOptions(budget = budget)
    }

    companion object {
        const val USER_GOAL_BINDING: String = "userGoal"

        private const val ACTION_LIMIT: Int = 50

        private const val SMART_GOAL_ACTION_LIMIT: Int = 75

        private const val TOKEN_LIMIT: Int = 1_000_000

        private const val COST_LIMIT: Double = 2.0

        private const val SMART_GOAL_COST_LIMIT: Double = 3.0
    }
}

private fun callCanvasModel(
    chatModel: ChatModel,
    prompt: String,
    toolCallbacks: List<ToolCallback>,
    tokenUsageAccumulator: TokenUsageAccumulator,
): String {
    var chatClientRequestSpec = ChatClient.create(chatModel)
        .prompt()
        .user(prompt)

    if (toolCallbacks.isNotEmpty()) {
        chatClientRequestSpec = chatClientRequestSpec.toolCallbacks(toolCallbacks)
    }

    val chatResponse = chatClientRequestSpec.call()
        .chatResponse()

    tokenUsageAccumulator.record(chatResponse)

    return chatResponse?.result
        ?.output
        ?.text
        ?: ""
}

private fun dynamicTypeNameFor(bindingName: String): String =
    bindingName.replaceFirstChar { firstChar -> firstChar.uppercaseChar() }

private fun renderInputContent(input: Any): String = when (input) {
    is Binding -> input.content
    is Map<*, *> -> objectMapper.writeValueAsString(
        input.filterKeys { key -> key != TYPE_NAME_KEY && key != TYPE_LABELS_KEY })
    else -> input.toString()
}

private fun typedOutputInstructions(outputType: DynamicType?): String {
    if (outputType == null) {
        return ""
    }

    return buildString {
        append("\n\nRespond with ONLY a JSON object — no markdown code fences, no commentary — ")
        append("containing exactly these properties:\n")

        for (property in outputType.ownProperties) {
            val propertyType = (property as? ValuePropertyDefinition)?.type ?: "string"

            append("- \"").append(property.name).append("\" (").append(propertyType).append(")")

            if (property.description.isNotBlank() && property.description != property.name) {
                append(": ").append(property.description)
            }

            append("\n")
        }
    }
}

private fun parseTypedOutput(
    responseText: String,
    outputType: DynamicType,
    actionName: String,
): Map<String, Any?> {
    val json = stripCodeFences(responseText)

    val parsed: Map<*, *> = try {
        objectMapper.readValue(json, Map::class.java)
    } catch (e: JacksonException) {
        throw IllegalStateException(
            "Action '$actionName' declares typed output '${outputType.name}' but the model did not return a " +
                "parseable JSON object. Response starts with: '${json.take(200)}'",
            e,
        )
    }

    val missingProperties = outputType.ownProperties
        .map { it.name }
        .filter { propertyName -> !parsed.containsKey(propertyName) }

    if (missingProperties.isNotEmpty()) {
        logger.warn(
            "Action '{}' produced typed output '{}' without declared properties {}; continuing with the " +
                "partial object.",
            actionName, outputType.name, missingProperties,
        )
    }

    val taggedMap = LinkedHashMap<String, Any?>(parsed.size + 1)

    taggedMap[TYPE_NAME_KEY] = outputType.name

    parsed.forEach { (key, value) ->
        if (key != TYPE_NAME_KEY && key != TYPE_LABELS_KEY) {
            taggedMap[key.toString()] = value
        }
    }

    return taggedMap
}

private fun stripCodeFences(text: String): String {
    val trimmed = text.trim()

    if (!trimmed.startsWith("```")) {
        return trimmed
    }

    val withoutOpeningFence = trimmed.substringAfter("\n", missingDelimiterValue = "")

    return withoutOpeningFence.substringBeforeLast("```").trim()
}

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
