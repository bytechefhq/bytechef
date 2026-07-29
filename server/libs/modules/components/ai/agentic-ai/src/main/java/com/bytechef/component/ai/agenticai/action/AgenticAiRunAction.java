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

package com.bytechef.component.ai.agenticai.action;

import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.ACTION_COST;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.ACTION_DESCRIPTION;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.ACTION_NAME;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.ACTION_PROMPT;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.DEFAULT_ACTION_COST;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.GOAL_DESCRIPTION;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.GOAL_MODE;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.GOAL_MODE_SMART;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.GOAL_MODE_STRUCTURAL;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.GOAL_OUTPUT_BINDING;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.INPUT_BINDING;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.OUTPUT_BINDING;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.OUTPUT_SCHEMA;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.OUTPUT_SCHEMA_DESCRIPTION;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.OUTPUT_SCHEMA_NAME;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.OUTPUT_SCHEMA_TYPE;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.RUN;
import static com.bytechef.component.ai.agenticai.constant.AgenticAiConstants.RUN_PROPERTIES;
import static com.bytechef.component.ai.llm.constant.LLMConstants.SYSTEM_PROMPT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.platform.component.definition.ai.agent.ActionFunction.ACTION;
import static com.bytechef.platform.component.definition.ai.agent.ModelFunction.MODEL;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.MapUtils;
import com.bytechef.component.ai.agenticai.embabel.ActionCompletionListener;
import com.bytechef.component.ai.agenticai.embabel.ActionStep;
import com.bytechef.component.ai.agenticai.embabel.Binding;
import com.bytechef.component.ai.agenticai.embabel.EmbabelAgentRunner;
import com.bytechef.component.ai.agenticai.embabel.OutputProperty;
import com.bytechef.component.ai.agenticai.facade.AgenticAiToolFacade;
import com.bytechef.component.ai.llm.util.ModelUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.ai.agent.BaseToolFunction;
import com.bytechef.platform.component.ComponentConnection;
import com.bytechef.platform.component.definition.AbstractActionDefinitionWrapper;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.component.definition.MultipleConnectionsOutputFunction;
import com.bytechef.platform.component.definition.MultipleConnectionsPerformFunction;
import com.bytechef.platform.component.definition.ParametersFactory;
import com.bytechef.platform.component.definition.ai.agent.ModelFunction;
import com.bytechef.platform.component.definition.ai.agent.ToolCallbackProviderFunction;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.configuration.domain.ClusterElement;
import com.bytechef.platform.configuration.domain.ClusterElementMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

/**
 * @author Ivica Cardic
 */
public class AgenticAiRunAction {

    private static final Logger log = LoggerFactory.getLogger(AgenticAiRunAction.class);

    private static final String BLACKBOARD_CHECKPOINT_DATA_KEY = "agenticAiBlackboardCheckpoint";
    private static final String CHECKPOINT_FINGERPRINT = "fingerprint";
    private static final String CHECKPOINT_BINDINGS = "bindings";

    private final EmbabelAgentRunner embabelAgentRunner;
    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final AgenticAiToolFacade agenticAiToolFacade;

    public static RunActionDefinitionWrapper of(
        EmbabelAgentRunner embabelAgentRunner, ClusterElementDefinitionService clusterElementDefinitionService,
        AgenticAiToolFacade agenticAiToolFacade) {

        return new AgenticAiRunAction(embabelAgentRunner, clusterElementDefinitionService, agenticAiToolFacade)
            .build();
    }

    private AgenticAiRunAction(
        EmbabelAgentRunner embabelAgentRunner, ClusterElementDefinitionService clusterElementDefinitionService,
        AgenticAiToolFacade agenticAiToolFacade) {

        this.embabelAgentRunner = embabelAgentRunner;
        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.agenticAiToolFacade = agenticAiToolFacade;
    }

    private RunActionDefinitionWrapper build() {
        return new RunActionDefinitionWrapper(
            action(RUN)
                .title("Run")
                .description(
                    "Run the agentic AI to autonomously achieve a goal. " +
                        "The GOAP planner selects and orders configured actions based on their input/output " +
                        "bindings, choosing any valid path from the seeded input binding to the goal output " +
                        "binding.")
                .properties(RUN_PROPERTIES)
                .output(
                    (MultipleConnectionsOutputFunction) (
                        inputParameters, componentConnections, extensions, context) -> ModelUtils.output(
                            inputParameters, null, context)));
    }

    @Nullable
    private Object perform(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters, Parameters extensions,
        ActionContext context) throws Exception {

        ClusterElementMap clusterElementMap = ClusterElementMap.of(extensions);

        List<ToolCallback> sharedToolCallbacks = getToolCallbacks(
            clusterElementMap.getClusterElements(BaseToolFunction.TOOLS), connectionParameters,
            context.isEditorEnvironment(), context);

        List<ActionStep> actionSteps = getActionSteps(
            clusterElementMap.getClusterElements(ACTION), sharedToolCallbacks, connectionParameters,
            context.isEditorEnvironment(), context);

        String goalDescription = inputParameters.getRequiredString(GOAL_DESCRIPTION);
        String goalOutputBinding = inputParameters.getRequiredString(GOAL_OUTPUT_BINDING);
        String goalMode = inputParameters.getString(GOAL_MODE, GOAL_MODE_STRUCTURAL);

        boolean smartGoal = GOAL_MODE_SMART.equals(goalMode);

        String systemPrompt = inputParameters.getString(SYSTEM_PROMPT);

        ChatModel chatModel = getChatModel(inputParameters, connectionParameters, clusterElementMap);

        Map<String, Object> seedBindings = fetchCheckpointedBindings(inputParameters, context);
        ActionCompletionListener actionCompletionListener = createBlackboardCheckpointer(inputParameters, context);

        Object result = embabelAgentRunner.run(
            actionSteps, goalDescription, goalOutputBinding, smartGoal, systemPrompt, chatModel, seedBindings,
            actionCompletionListener);

        clearBlackboardCheckpoint(context);

        return result;
    }

    /**
     * Creates the per-action blackboard checkpointer for durable (non-editor, job-attached) executions, or {@code null}
     * when checkpointing does not apply. After every completed GOAP action the produced bindings collected so far are
     * written to execution-scoped data storage together with a fingerprint of the evaluated input parameters, so a
     * crash-resumed job replays a checkpoint only when it was written by this same agentic node configuration. Failures
     * are swallowed by the runner — a storage problem never fails the plan.
     */
    private static @Nullable ActionCompletionListener createBlackboardCheckpointer(
        Parameters inputParameters, ActionContext context) {

        if (!(context instanceof ActionContextAware actionContextAware) || actionContextAware.getJobId() == null ||
            actionContextAware.isEditorEnvironment()) {

            return null;
        }

        Map<String, Object> producedBindings = new ConcurrentHashMap<>();

        return (bindingName, value) -> {
            producedBindings.put(bindingName, value instanceof Binding binding ? binding.getContent() : value);

            context.data(
                data -> data.put(
                    ActionContext.Data.Scope.CURRENT_EXECUTION, BLACKBOARD_CHECKPOINT_DATA_KEY,
                    Map.of(
                        CHECKPOINT_FINGERPRINT, getRunFingerprint(inputParameters),
                        CHECKPOINT_BINDINGS, new HashMap<>(producedBindings))));
        };
    }

    /**
     * Returns the bindings restored from a crash checkpoint left by a previous, interrupted run of this job, or an
     * empty map when there is none (or it belongs to a differently-parameterized node). Restoration is best-effort: any
     * failure logs and falls back to running the plan from scratch.
     */
    private static Map<String, Object> fetchCheckpointedBindings(Parameters inputParameters, ActionContext context) {
        if (!(context instanceof ActionContextAware actionContextAware) || actionContextAware.getJobId() == null ||
            actionContextAware.isEditorEnvironment()) {

            return Map.of();
        }

        try {
            Optional<Object> checkpointOptional = context.data(
                data -> data.fetch(ActionContext.Data.Scope.CURRENT_EXECUTION, BLACKBOARD_CHECKPOINT_DATA_KEY));

            if (checkpointOptional.isEmpty() || !(checkpointOptional.get() instanceof Map<?, ?> checkpointMap)) {
                return Map.of();
            }

            if (!Objects.equals(checkpointMap.get(CHECKPOINT_FINGERPRINT), getRunFingerprint(inputParameters))) {
                return Map.of();
            }

            if (!(checkpointMap.get(CHECKPOINT_BINDINGS) instanceof Map<?, ?> bindingsMap)) {
                return Map.of();
            }

            Map<String, Object> seedBindings = new HashMap<>();

            bindingsMap.forEach((bindingName, value) -> {
                if (bindingName != null && value != null) {
                    seedBindings.put(bindingName.toString(), value);
                }
            });

            if (!seedBindings.isEmpty()) {
                int seededBindingCount = seedBindings.size();

                context.log(logger -> logger.info(
                    "Resuming agentic plan from crash checkpoint (%d completed binding(s))".formatted(
                        seededBindingCount)));
            }

            return seedBindings;
        } catch (RuntimeException exception) {
            log.warn("Failed to restore agentic blackboard checkpoint; running the plan from scratch", exception);

            return Map.of();
        }
    }

    /** Removes this job's blackboard checkpoint after the plan completed successfully. */
    private static void clearBlackboardCheckpoint(ActionContext context) {
        if (!(context instanceof ActionContextAware actionContextAware) || actionContextAware.getJobId() == null ||
            actionContextAware.isEditorEnvironment()) {

            return;
        }

        try {
            context.data(
                data -> data.remove(ActionContext.Data.Scope.CURRENT_EXECUTION, BLACKBOARD_CHECKPOINT_DATA_KEY));
        } catch (RuntimeException exception) {
            log.warn("Failed to clear agentic blackboard checkpoint", exception);
        }
    }

    private static String getRunFingerprint(Parameters inputParameters) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

            byte[] digest = messageDigest.digest(
                JsonUtils.write(inputParameters.toMap())
                    .getBytes(StandardCharsets.UTF_8));

            HexFormat hexFormat = HexFormat.of();

            return hexFormat.formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    /**
     * Resolves the canvas-selected MODEL cluster element into a Spring AI {@link ChatModel}. All the agent's LLM calls
     * — action prompts and smart-goal evaluations — run against this model, so the component works without Embabel's
     * own model registry (and without any provider API key beyond the model's ByteChef connection).
     */
    private ChatModel getChatModel(
        Parameters inputParameters, Map<String, ComponentConnection> connectionParameters,
        ClusterElementMap clusterElementMap) throws Exception {

        ClusterElement modelClusterElement = clusterElementMap.fetchClusterElement(MODEL)
            .orElseThrow(() -> new IllegalArgumentException(
                "The Agentic AI component requires a Model: attach a model cluster element (e.g. OpenAI, " +
                    "Anthropic) with its connection."));

        ModelFunction modelFunction = clusterElementDefinitionService.getClusterElement(
            modelClusterElement.getComponentName(), modelClusterElement.getComponentVersion(),
            modelClusterElement.getClusterElementName());

        ComponentConnection modelConnection = connectionParameters.get(modelClusterElement.getWorkflowNodeName());

        if (modelConnection == null) {
            throw new IllegalArgumentException(
                "The Agentic AI component's model '" + modelClusterElement.getWorkflowNodeName() +
                    "' has no connection selected.");
        }

        Map<String, Object> concatenatedInputParameters = MapUtils.concat(
            new HashMap<>(inputParameters.toMap()), new HashMap<>(modelClusterElement.getParameters()));

        return (ChatModel) modelFunction.apply(
            ParametersFactory.create(concatenatedInputParameters),
            ParametersFactory.create(modelConnection.getParameters()), false);
    }

    /**
     * Builds the per-action step list. Tools attached to the agentic-ai action itself (shared tools) are available to
     * every action. Additionally, if an individual action's cluster extensions declare their own nested {@code TOOLS}
     * cluster elements, those are appended to that action's tool list — giving per-action tool scoping when the canvas
     * supports it, while preserving the shared-tools behavior as the default.
     */
    private List<ActionStep> getActionSteps(
        List<ClusterElement> actionClusterElements, List<ToolCallback> sharedToolCallbacks,
        Map<String, ComponentConnection> connectionParameters, boolean editorEnvironment, ActionContext context)
        throws Exception {

        List<ActionStep> actionSteps = new ArrayList<>();

        for (ClusterElement clusterElement : actionClusterElements) {
            Map<String, ?> parameters = clusterElement.getParameters();
            String workflowNodeName = clusterElement.getWorkflowNodeName();

            String actionName = requireStringParameter(parameters, ACTION_NAME, workflowNodeName);
            String inputBinding = requireStringParameter(parameters, INPUT_BINDING, workflowNodeName);
            String outputBinding = requireStringParameter(parameters, OUTPUT_BINDING, workflowNodeName);
            String actionPrompt = requireStringParameter(parameters, ACTION_PROMPT, workflowNodeName);
            String actionDescription = (String) parameters.get(ACTION_DESCRIPTION);

            double actionCost = parseActionCost(parameters.get(ACTION_COST), workflowNodeName);

            List<OutputProperty> outputProperties = parseOutputSchema(parameters.get(OUTPUT_SCHEMA),
                workflowNodeName);

            List<ToolCallback> stepToolCallbacks = resolveStepToolCallbacks(
                clusterElement, sharedToolCallbacks, connectionParameters, editorEnvironment, context);

            actionSteps.add(
                new ActionStep(actionName, actionDescription, actionPrompt, inputBinding, outputBinding,
                    stepToolCallbacks, actionCost, outputProperties));
        }

        return actionSteps;
    }

    /**
     * Parses the optional output schema declared on an action cluster element into the runner's {@link OutputProperty}
     * list. A declared schema turns the action's output binding into a typed binding: the model is instructed to
     * produce a JSON object with these properties, and the value travels the blackboard as a type-tagged map instead of
     * free text.
     */
    private static List<OutputProperty> parseOutputSchema(@Nullable Object rawOutputSchema, String workflowNodeName) {
        if (rawOutputSchema == null) {
            return List.of();
        }

        if (!(rawOutputSchema instanceof List<?> schemaEntries)) {
            throw new IllegalArgumentException(
                "Agentic AI action '" + workflowNodeName + "' has a malformed '" + OUTPUT_SCHEMA +
                    "' value; expected a list of property definitions");
        }

        List<OutputProperty> outputProperties = new ArrayList<>();

        for (Object schemaEntry : schemaEntries) {
            if (!(schemaEntry instanceof Map<?, ?> schemaEntryMap)) {
                throw new IllegalArgumentException(
                    "Agentic AI action '" + workflowNodeName + "' has a malformed '" + OUTPUT_SCHEMA +
                        "' entry: " + schemaEntry);
            }

            Object rawPropertyName = schemaEntryMap.get(OUTPUT_SCHEMA_NAME);

            if (!(rawPropertyName instanceof String propertyName) || propertyName.isBlank()) {
                throw new IllegalArgumentException(
                    "Agentic AI action '" + workflowNodeName + "' has an '" + OUTPUT_SCHEMA +
                        "' entry without a property name");
            }

            String propertyType = schemaEntryMap.get(OUTPUT_SCHEMA_TYPE) instanceof String typeValue &&
                !typeValue.isBlank() ? typeValue : "string";
            String propertyDescription = schemaEntryMap.get(
                OUTPUT_SCHEMA_DESCRIPTION) instanceof String descriptionValue &&
                !descriptionValue.isBlank() ? descriptionValue : null;

            outputProperties.add(new OutputProperty(propertyName.trim(), propertyType, propertyDescription));
        }

        return outputProperties;
    }

    private static String requireStringParameter(
        Map<String, ?> parameters, String key, String workflowNodeName) {

        Object value = parameters.get(key);

        if (!(value instanceof String stringValue) || stringValue.isBlank()) {
            throw new IllegalArgumentException(
                "Agentic AI action '" + workflowNodeName + "' is missing required field '" + key + "'");
        }

        return stringValue;
    }

    private static double parseActionCost(@Nullable Object rawCost, String workflowNodeName) {
        if (rawCost == null) {
            return DEFAULT_ACTION_COST;
        }

        if (rawCost instanceof Number number) {
            return number.doubleValue();
        }

        log.warn(
            "Agentic AI action '{}' has non-numeric {} value '{}'; falling back to default cost {}",
            workflowNodeName, ACTION_COST, rawCost, DEFAULT_ACTION_COST);

        return DEFAULT_ACTION_COST;
    }

    private List<ToolCallback> resolveStepToolCallbacks(
        ClusterElement actionClusterElement, List<ToolCallback> sharedToolCallbacks,
        Map<String, ComponentConnection> connectionParameters, boolean editorEnvironment, ActionContext context)
        throws Exception {

        Map<String, ?> actionExtensions = actionClusterElement.getExtensions();

        if (actionExtensions == null || actionExtensions.isEmpty()) {
            return sharedToolCallbacks;
        }

        List<ClusterElement> nestedToolElements = ClusterElementMap.of(actionExtensions)
            .getClusterElements(BaseToolFunction.TOOLS);

        if (nestedToolElements.isEmpty()) {
            return sharedToolCallbacks;
        }

        List<ToolCallback> stepToolCallbacks = new ArrayList<>(sharedToolCallbacks);

        stepToolCallbacks.addAll(
            getToolCallbacks(nestedToolElements, connectionParameters, editorEnvironment, context));

        return stepToolCallbacks;
    }

    private List<ToolCallback> getToolCallbacks(
        List<ClusterElement> toolClusterElements, Map<String, ComponentConnection> connectionParameters,
        boolean editorEnvironment, ActionContext context) throws Exception {

        List<ToolCallback> toolCallbacks = new ArrayList<>();

        for (ClusterElement clusterElement : toolClusterElements) {
            try {
                toolCallbacks.addAll(
                    resolveClusterElementToolCallbacks(
                        clusterElement, connectionParameters, editorEnvironment, context));
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to register tool for cluster element '" + clusterElement.getClusterElementName() +
                        "' on component '" + clusterElement.getComponentName() +
                        "' (workflow node '" + clusterElement.getWorkflowNodeName() + "'): " + e.getMessage(),
                    e);
            }
        }

        return toolCallbacks;
    }

    private List<ToolCallback> resolveClusterElementToolCallbacks(
        ClusterElement clusterElement, Map<String, ComponentConnection> connectionParameters,
        boolean editorEnvironment, ActionContext context) throws Exception {

        Object clusterElementFunction = clusterElementDefinitionService.getClusterElement(
            clusterElement.getComponentName(), clusterElement.getComponentVersion(),
            clusterElement.getClusterElementName());

        ComponentConnection componentConnection = connectionParameters.get(clusterElement.getWorkflowNodeName());

        if (clusterElementFunction instanceof ToolCallbackProviderFunction toolCallbackProviderFunction) {
            ToolCallbackProvider toolCallbackProvider = toolCallbackProviderFunction.apply(
                ParametersFactory.create(clusterElement.getParameters()),
                ParametersFactory.create(componentConnection), context);

            ToolCallback[] providerCallbacks = toolCallbackProvider.getToolCallbacks();

            if (providerCallbacks.length == 0) {
                log.warn(
                    "Tool callback provider '{}' on component '{}' (workflow node '{}') returned no callbacks; " +
                        "the LLM will not see any tools from this cluster element.",
                    clusterElement.getClusterElementName(), clusterElement.getComponentName(),
                    clusterElement.getWorkflowNodeName());
            }

            return Arrays.asList(providerCallbacks);
        }

        return List.of(
            agenticAiToolFacade.getFunctionToolCallback(clusterElement, componentConnection, editorEnvironment));
    }

    public class RunActionDefinitionWrapper extends AbstractActionDefinitionWrapper {

        public RunActionDefinitionWrapper(ActionDefinition actionDefinition) {
            super(actionDefinition);
        }

        @Override
        public Optional<? extends BasePerformFunction> getPerform() {
            return Optional.of((MultipleConnectionsPerformFunction) AgenticAiRunAction.this::perform);
        }
    }
}
