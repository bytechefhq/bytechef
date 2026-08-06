/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.codeworkflow.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.ee.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.ee.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.ee.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.constant.WorkflowExtConstants;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.workflow.definition.CompositeTaskDefinition;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.Input;
import com.bytechef.workflow.definition.Output;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.TriggerDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.bytechef.workflow.definition.WorkflowTaskDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
public class CodeWorkflowContainerFacadeImpl implements CodeWorkflowContainerFacade {

    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final @Nullable ComponentDefinitionService componentDefinitionService;
    private final ObjectMapper objectMapper;
    private final WorkflowService workflowService;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public CodeWorkflowContainerFacadeImpl(
        CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
        ObjectProvider<ComponentDefinitionService> componentDefinitionServiceProvider, ObjectMapper objectMapper,
        WorkflowService workflowService) {

        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.componentDefinitionService = componentDefinitionServiceProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        this.workflowService = workflowService;
    }

    @Override
    public CodeWorkflowContainer create(
        String name, String externalVersion, List<WorkflowDefinition> workflowDefinitions, Language language,
        byte[] bytes, PlatformType type) {

        return create(name, externalVersion, workflowDefinitions, language, bytes, type, Map.of())
            .codeWorkflowContainer();
    }

    @Override
    public CodeWorkflowReconciliation create(
        String name, String externalVersion, List<WorkflowDefinition> workflowDefinitions, Language language,
        byte[] bytes, PlatformType type, Map<String, String> reusableWorkflowNameIds) {

        try {
            UUID codeWorkflowContainerUuid = UUID.randomUUID();

            CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(codeWorkflowContainerUuid);

            Map<String, String> addedWorkflowNameIds = new HashMap<>();

            for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
                String definition = getDefinition(String.valueOf(codeWorkflowContainerUuid), workflowDefinition, type);

                String reusableWorkflowId = reusableWorkflowNameIds.get(workflowDefinition.getName());

                if (reusableWorkflowId == null) {
                    Workflow workflow = workflowService.create(
                        definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

                    addedWorkflowNameIds.put(workflowDefinition.getName(), workflow.getId());
                    codeWorkflowContainer.addCodeWorkflow(
                        UUID.fromString(Objects.requireNonNull(workflow.getId())), workflowDefinition.getName());
                } else {
                    Workflow workflow = workflowService.getWorkflow(reusableWorkflowId);

                    workflowService.update(reusableWorkflowId, definition, workflow.getVersion());

                    codeWorkflowContainer.addCodeWorkflow(
                        UUID.fromString(reusableWorkflowId), workflowDefinition.getName());
                }
            }

            Map<String, String> removedWorkflowNameIds = removedFrom(reusableWorkflowNameIds, workflowDefinitions);

            codeWorkflowContainer.setExternalVersion(externalVersion);
            codeWorkflowContainer.setLanguage(language);
            codeWorkflowContainer.setName(name);

            FileEntry workflowsFileEntry = codeWorkflowFileStorage.storeCodeWorkflowFile(
                codeWorkflowContainerUuid + "." + language.getExtension(), bytes);

            codeWorkflowContainer.setWorkflows(workflowsFileEntry);

            return new CodeWorkflowReconciliation(
                codeWorkflowContainerService.create(codeWorkflowContainer), addedWorkflowNameIds,
                removedWorkflowNameIds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CodeWorkflowReconciliation update(
        CodeWorkflowContainer codeWorkflowContainer, String externalVersion,
        List<WorkflowDefinition> workflowDefinitions, byte[] bytes, PlatformType type) {

        try {
            Map<String, String> existingWorkflowNameIds = codeWorkflowContainer.getWorkflowNameIds();
            Map<String, String> addedWorkflowNameIds = new HashMap<>();

            for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
                String definition = getDefinition(
                    String.valueOf(codeWorkflowContainer.getUuid()), workflowDefinition, type);

                String existingWorkflowId = existingWorkflowNameIds.get(workflowDefinition.getName());

                if (existingWorkflowId == null) {
                    Workflow workflow = workflowService.create(
                        definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

                    addedWorkflowNameIds.put(workflowDefinition.getName(), workflow.getId());
                    codeWorkflowContainer.addCodeWorkflow(
                        UUID.fromString(Objects.requireNonNull(workflow.getId())), workflowDefinition.getName());
                } else {
                    Workflow workflow = workflowService.getWorkflow(existingWorkflowId);

                    workflowService.update(existingWorkflowId, definition, workflow.getVersion());
                }
            }

            Map<String, String> removedWorkflowNameIds = removedFrom(existingWorkflowNameIds, workflowDefinitions);

            for (String workflowName : removedWorkflowNameIds.keySet()) {
                codeWorkflowContainer.removeCodeWorkflow(workflowName);
            }

            codeWorkflowContainer.setExternalVersion(externalVersion);

            FileEntry oldWorkflowsFileEntry = codeWorkflowContainer.getWorkflows();

            FileEntry workflowsFileEntry = codeWorkflowFileStorage.storeCodeWorkflowFile(
                codeWorkflowContainer.getUuid() + "." + codeWorkflowContainer.getLanguage()
                    .getExtension(),
                bytes);

            codeWorkflowContainer.setWorkflows(workflowsFileEntry);

            CodeWorkflowContainer updatedCodeWorkflowContainer = codeWorkflowContainerService.update(
                codeWorkflowContainer);

            if (oldWorkflowsFileEntry != null &&
                !Objects.equals(oldWorkflowsFileEntry.getUrl(), workflowsFileEntry.getUrl())) {

                codeWorkflowFileStorage.deleteCodeWorkflowFile(oldWorkflowsFileEntry);
            }

            return new CodeWorkflowReconciliation(
                updatedCodeWorkflowContainer, addedWorkflowNameIds, removedWorkflowNameIds);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, String> removedFrom(
        Map<String, String> knownWorkflowNameIds, List<WorkflowDefinition> workflowDefinitions) {

        Set<String> incomingNames = workflowDefinitions.stream()
            .map(WorkflowDefinition::getName)
            .collect(Collectors.toSet());

        return knownWorkflowNameIds.entrySet()
            .stream()
            .filter(entry -> !incomingNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private ArrayNode toArrayNode(
        String codeWorkflowContainerUuid, WorkflowDefinition workflowDefinition,
        List<? extends WorkflowTaskDefinition> tasks, PlatformType type) {

        ArrayNode arrayNode = objectMapper.createArrayNode();

        for (WorkflowTaskDefinition taskDefinition : tasks) {
            if (taskDefinition instanceof CompositeTaskDefinition compositeTaskDefinition) {
                arrayNode.add(
                    toCompositeTaskNode(
                        codeWorkflowContainerUuid, workflowDefinition, compositeTaskDefinition, type));
            } else {
                arrayNode.add(
                    toTaskNode(
                        codeWorkflowContainerUuid, workflowDefinition, (TaskDefinition) taskDefinition, type,
                        List.of()));
            }
        }

        return arrayNode;
    }

    /**
     * Emits the engine's concurrency dispatchers around the group's leaf tasks: {@code parallel/v1} takes a flat
     * {@code tasks} list dispatched at once, {@code fork-join/v1} takes {@code branches}, each a sequence. The leaves
     * are ordinary perform nodes, so a nested task keeps its own connections and its own job-context snapshot.
     */
    private ObjectNode toCompositeTaskNode(
        String codeWorkflowContainerUuid, WorkflowDefinition workflowDefinition,
        CompositeTaskDefinition compositeTaskDefinition, PlatformType type) {

        ObjectNode taskNode = objectMapper.createObjectNode()
            .put("description", OptionalUtils.orElse(compositeTaskDefinition.getDescription(), null))
            .put("label", OptionalUtils.orElse(compositeTaskDefinition.getLabel(), null))
            .put("name", compositeTaskDefinition.getName());

        ObjectNode parametersNode = objectMapper.createObjectNode();

        if (compositeTaskDefinition.getType() == CompositeTaskDefinition.Type.PARALLEL) {
            taskNode.put("type", "parallel/v1");

            ArrayNode tasksNode = objectMapper.createArrayNode();

            List<String> groupTaskNames = compositeTaskDefinition.getTasks()
                .stream()
                .map(TaskDefinition::getName)
                .toList();

            for (TaskDefinition taskDefinition : compositeTaskDefinition.getTasks()) {
                tasksNode.add(
                    toTaskNode(
                        codeWorkflowContainerUuid, workflowDefinition, taskDefinition, type,
                        without(groupTaskNames, taskDefinition.getName())));
            }

            parametersNode.set("tasks", tasksNode);
        } else {
            taskNode.put("type", "fork-join/v1");

            ArrayNode branchesNode = objectMapper.createArrayNode();

            List<String> groupTaskNames = compositeTaskDefinition.getBranches()
                .stream()
                .flatMap(List::stream)
                .map(TaskDefinition::getName)
                .toList();

            for (List<? extends TaskDefinition> branch : compositeTaskDefinition.getBranches()) {
                ArrayNode branchNode = objectMapper.createArrayNode();

                // A branch runs its own tasks in sequence, so only the other branches' tasks are concurrent.
                List<String> branchTaskNames = branch.stream()
                    .map(TaskDefinition::getName)
                    .toList();

                List<String> concurrentTaskNames = groupTaskNames.stream()
                    .filter(taskName -> !branchTaskNames.contains(taskName))
                    .toList();

                for (TaskDefinition taskDefinition : branch) {
                    branchNode.add(
                        toTaskNode(
                            codeWorkflowContainerUuid, workflowDefinition, taskDefinition, type, concurrentTaskNames));
                }

                branchesNode.add(branchNode);
            }

            parametersNode.set("branches", branchesNode);
        }

        taskNode.set("parameters", parametersNode);

        return taskNode;
    }

    private static List<String> without(List<String> names, String name) {
        return names.stream()
            .filter(curName -> !curName.equals(name))
            .toList();
    }

    private ObjectNode toTaskNode(
        String codeWorkflowContainerUuid, WorkflowDefinition workflowDefinition, TaskDefinition taskDefinition,
        PlatformType type, List<String> concurrentTaskNames) {

        ObjectNode taskNode = objectMapper.createObjectNode()
            .put("description", OptionalUtils.orElse(taskDefinition.getDescription(), null))
            .put("label", OptionalUtils.orElse(taskDefinition.getLabel(), null))
            .put("name", taskDefinition.getName())
            .put("type", "codeWorkflow/v1/perform");

        // A task property the workflow mapper does not recognize lands in WorkflowTask.extensions, which is
        // where ComponentConnection.of(...) reads the connections map from — so this is emitted at the task's
        // top level, NOT nested under an "extensions" key (that would fail reserved-word validation).
        OptionalUtils.ifPresent(
            taskDefinition.getConnections(),
            connections -> taskNode.set(WorkflowExtConstants.CONNECTIONS, toConnectionsNode(connections)));

        // "=#root" evaluates to the whole job context — the workflow's inputs plus the output of every task that
        // already ran — which is what TaskContext.input() hands the task. It is a formula rather than a generated
        // map of ${taskName} references because a task name is free-form here: a hyphen makes it unreferenceable
        // as an accessor expression, and the platform would reject the workflow at save time.
        ObjectNode parametersNode = objectMapper.createObjectNode();

        // The task's own declared parameters go on first, so the engine evaluates any ${...} in them against the job
        // context; the platform's own keys are written after and win, since a task cannot redefine how it is
        // dispatched.
        OptionalUtils.ifPresent(
            taskDefinition.getParameters(),
            parameters -> parametersNode.setAll((ObjectNode) objectMapper.valueToTree(parameters)));

        parametersNode
            .put("codeWorkflowContainerUuid", codeWorkflowContainerUuid)
            .put("workflowName", workflowDefinition.getName())
            .put("taskName", taskDefinition.getName())
            .put("input", "=#root")
            .put("type", type.ordinal());

        if (!concurrentTaskNames.isEmpty()) {
            ArrayNode concurrentTaskNamesNode = objectMapper.createArrayNode();

            concurrentTaskNames.forEach(concurrentTaskNamesNode::add);

            parametersNode.set("concurrentTaskNames", concurrentTaskNamesNode);
        }

        taskNode.set("parameters", parametersNode);

        return taskNode;
    }

    /**
     * Builds the {@code extensions.connections} map the platform's {@code ComponentConnectionFactory} chain reads:
     * keyed by the declared connection name (which is also the {@code connectionName} the task's perform passes), each
     * entry carrying the component name and version. An unpinned declaration resolves to the component's latest version
     * at save time, mirroring how a visual workflow node pins its component version in its type.
     */
    private ObjectNode toConnectionsNode(List<? extends ConnectionRequirement> connections) {
        ObjectNode connectionsNode = objectMapper.createObjectNode();

        for (ConnectionRequirement connectionRequirement : connections) {
            String componentName = connectionRequirement.getComponentName();

            OptionalInt componentVersion = connectionRequirement.getComponentVersion();

            connectionsNode.set(
                connectionRequirement.getName(),
                objectMapper.createObjectNode()
                    .put(WorkflowExtConstants.COMPONENT_NAME, componentName)
                    .put(
                        WorkflowExtConstants.COMPONENT_VERSION,
                        componentVersion.isPresent() ? componentVersion.getAsInt()
                            : resolveLatestComponentVersion(componentName)));
        }

        return connectionsNode;
    }

    private int resolveLatestComponentVersion(String componentName) {
        if (componentDefinitionService == null) {
            return 1;
        }

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            componentName, null);

        return componentDefinition.getVersion();
    }

    /**
     * Emits the workflow's declared inputs as the ARRAY the workflow model reads — the shape the platform's own
     * {@code Workflow.Input} list expects, and what test configuration prompts from.
     */
    private ArrayNode toInputsNode(List<? extends Input> inputs) {
        ArrayNode inputsNode = objectMapper.createArrayNode();

        for (Input input : inputs) {
            ObjectNode inputNode = objectMapper.createObjectNode()
                .put("name", input.getName())
                .put("type", input.getType())
                .put("required", input.isRequired());

            if (input.getLabel() != null) {
                inputNode.put("label", input.getLabel());
            }

            inputsNode.add(inputNode);
        }

        return inputsNode;
    }

    /**
     * Emits the workflow's declared outputs. Naming a task becomes the {@code =#root['<task>']} formula rather than a
     * {@code ${task}} reference, so an output can name a task whose name is not a plain identifier — the same reason
     * the task input snapshot is a formula.
     */
    private ArrayNode toOutputsNode(List<? extends Output> outputs) {
        ArrayNode outputsNode = objectMapper.createArrayNode();

        for (Output output : outputs) {
            ObjectNode outputNode = objectMapper.createObjectNode()
                .put("name", output.getName());

            if (output.getTask() != null) {
                outputNode.put("value", "=#root['" + output.getTask() + "']");
            } else {
                outputNode.putPOJO("value", output.getValue());
            }

            outputsNode.add(outputNode);
        }

        return outputsNode;
    }

    /**
     * Emits the workflow's declared triggers as the platform's own trigger nodes — {@code name}, {@code type} and
     * {@code parameters} — which is what {@code WorkflowTrigger} parses. A trigger is a component the platform already
     * provides, so nothing here is guest code: a code workflow starts exactly the way a visual one does.
     */
    private ArrayNode toTriggersNode(List<? extends TriggerDefinition> triggers) {
        ArrayNode triggersNode = objectMapper.createArrayNode();

        for (TriggerDefinition trigger : triggers) {
            validateTriggerType(trigger);

            ObjectNode triggerNode = objectMapper.createObjectNode()
                .put("name", trigger.getName())
                .put("type", trigger.getType());

            triggerNode.set("parameters", objectMapper.valueToTree(trigger.getParameters()));

            triggersNode.add(triggerNode);
        }

        return triggersNode;
    }

    /**
     * Fails a save whose trigger names a component this instance does not have. A trigger that does not resolve is not
     * an error the platform reports later — the workflow simply never fires — so a typo has to be caught here.
     */
    private void validateTriggerType(TriggerDefinition trigger) {
        String type = trigger.getType();

        if (type == null || componentDefinitionService == null) {
            return;
        }

        String[] typeParts = type.split("/");

        if (typeParts.length != 3) {
            throw new IllegalArgumentException(
                "Trigger %s declares type %s; a trigger type reads <component>/v<version>/<trigger>, e.g. "
                    .formatted(trigger.getName(), type) + "schedule/v1/interval");
        }

        if (componentDefinitionService.fetchComponentDefinition(typeParts[0], null)
            .isEmpty()) {

            throw new IllegalArgumentException(
                "Trigger %s names component %s, which this instance does not have"
                    .formatted(trigger.getName(), typeParts[0]));
        }
    }

    private String getDefinition(
        String codeWorkflowContainerUuid, WorkflowDefinition workflowDefinition, PlatformType type) {

        ObjectNode objectNode = objectMapper.createObjectNode();

        OptionalUtils.ifPresent(workflowDefinition.getLabel(), label -> objectNode.put("label", label));
        OptionalUtils.ifPresent(workflowDefinition.getDescription(),
            description -> objectNode.put("description", description));
        OptionalUtils.ifPresent(workflowDefinition.getInputs(),
            inputs -> objectNode.set("inputs", toInputsNode(inputs)));
        OptionalUtils.ifPresent(
            workflowDefinition.getOutputs(), outputs -> objectNode.set("outputs", toOutputsNode(outputs)));
        OptionalUtils.ifPresent(
            workflowDefinition.getTriggers(), triggers -> objectNode.set("triggers", toTriggersNode(triggers)));
        OptionalUtils.ifPresent(
            workflowDefinition.getTasks(),
            tasks -> objectNode.set("tasks", toArrayNode(codeWorkflowContainerUuid, workflowDefinition, tasks, type)));

        return objectMapper.writeValueAsString(objectNode);
    }
}
