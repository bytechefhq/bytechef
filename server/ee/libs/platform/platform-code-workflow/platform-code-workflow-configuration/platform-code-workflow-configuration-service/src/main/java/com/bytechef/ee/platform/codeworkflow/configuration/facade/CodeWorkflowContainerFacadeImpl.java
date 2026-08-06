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
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.workflow.definition.ConnectionRequirement;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(CodeWorkflowContainerFacadeImpl.class);

    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final @Nullable ConnectionService connectionService;
    private final ObjectMapper objectMapper;
    private final WorkflowService workflowService;

    @SuppressFBWarnings({
        "CT_CONSTRUCTOR_THROW", "EI"
    })
    public CodeWorkflowContainerFacadeImpl(
        CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
        ObjectProvider<ConnectionService> connectionServiceProvider, ObjectMapper objectMapper,
        WorkflowService workflowService) {

        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.connectionService = connectionServiceProvider.getIfAvailable();
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

        warnOnMissingDeclaredConnections(workflowDefinitions, type);

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

        warnOnMissingDeclaredConnections(workflowDefinitions, type);

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
        String codeWorkflowContainerUuid, WorkflowDefinition workflowDefinition, List<? extends TaskDefinition> tasks,
        PlatformType type) {

        ArrayNode arrayNode = objectMapper.createArrayNode();

        for (TaskDefinition taskDefinition : tasks) {
            ObjectNode taskNode = objectMapper.createObjectNode()
                .put("description", OptionalUtils.orElse(taskDefinition.getDescription(), null))
                .put("label", OptionalUtils.orElse(taskDefinition.getLabel(), null))
                .put("name", taskDefinition.getName())
                .put("type", "codeWorkflow/v1/perform");

            OptionalUtils.ifPresent(
                taskDefinition.getConnections(), connections -> taskNode.set("connections", toConnectionsNode(
                    connections)));

            // TODO taskDefinition.getParameters()
            taskNode.set(
                "parameters",
                objectMapper.createObjectNode()
                    .put("codeWorkflowContainerUuid", codeWorkflowContainerUuid)
                    .put("workflowName", workflowDefinition.getName())
                    .put("taskName", taskDefinition.getName())
                    .put("type", type.ordinal()));

            arrayNode.add(taskNode);
        }

        return arrayNode;
    }

    /**
     * Returns a human-readable entry for every declared task connection whose name has no matching connection in the
     * store (any environment — declared names bind per environment at run time, so save/deploy can only check
     * existence, not the concrete environment binding). Empty when no {@link ConnectionService} bean is available.
     */
    List<String> findMissingDeclaredConnections(List<WorkflowDefinition> workflowDefinitions, PlatformType type) {
        if (connectionService == null) {
            return List.of();
        }

        List<String> missingConnections = new ArrayList<>();

        for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
            List<? extends TaskDefinition> tasks = OptionalUtils.orElse(workflowDefinition.getTasks(), List.of());

            for (TaskDefinition taskDefinition : tasks) {
                List<? extends ConnectionRequirement> connections = OptionalUtils.orElse(
                    taskDefinition.getConnections(), List.of());

                for (ConnectionRequirement connectionRequirement : connections) {
                    if (!declaredConnectionExists(connectionRequirement, type)) {
                        missingConnections.add(
                            "workflow=%s, task=%s, component=%s, connection=%s".formatted(
                                workflowDefinition.getName(), taskDefinition.getName(),
                                connectionRequirement.getComponentName(), connectionRequirement.getName()));
                    }
                }
            }
        }

        return missingConnections;
    }

    private boolean declaredConnectionExists(ConnectionRequirement connectionRequirement, PlatformType type) {
        OptionalInt componentVersion = connectionRequirement.getComponentVersion();

        List<Connection> connections = connectionService.getConnections(
            connectionRequirement.getComponentName(),
            componentVersion.isPresent() ? componentVersion.getAsInt() : null, null, null, type);

        return connections.stream()
            .anyMatch(connection -> Objects.equals(connection.getName(), connectionRequirement.getName()));
    }

    private void warnOnMissingDeclaredConnections(List<WorkflowDefinition> workflowDefinitions, PlatformType type) {
        for (String missingConnection : findMissingDeclaredConnections(workflowDefinitions, type)) {
            log.warn("Declared code workflow connection has no matching store connection: {}", missingConnection);
        }
    }

    private ArrayNode toConnectionsNode(List<? extends ConnectionRequirement> connections) {
        ArrayNode connectionsNode = objectMapper.createArrayNode();

        for (ConnectionRequirement connectionRequirement : connections) {
            ObjectNode connectionNode = objectMapper.createObjectNode()
                .put("componentName", connectionRequirement.getComponentName())
                .put("name", connectionRequirement.getName());

            OptionalInt componentVersion = connectionRequirement.getComponentVersion();

            componentVersion.ifPresent(version -> connectionNode.put("componentVersion", version));

            connectionsNode.add(connectionNode);
        }

        return connectionsNode;
    }

    private String getDefinition(
        String codeWorkflowContainerUuid, WorkflowDefinition workflowDefinition, PlatformType type) {

        ObjectNode objectNode = objectMapper.createObjectNode();

        OptionalUtils.ifPresent(workflowDefinition.getLabel(), label -> objectNode.put("label", label));
        OptionalUtils.ifPresent(workflowDefinition.getDescription(),
            description -> objectNode.put("description", description));
        OptionalUtils.ifPresent(
            workflowDefinition.getInputs(), inputs -> objectNode.set("inputs", objectMapper.createObjectNode()));
        OptionalUtils.ifPresent(
            workflowDefinition.getOutputs(), outputs -> objectNode.set("outputs", objectMapper.createObjectNode()));
        OptionalUtils.ifPresent(
            workflowDefinition.getTriggers(), triggers -> objectNode.set("triggers", objectMapper.createArrayNode()));
        OptionalUtils.ifPresent(
            workflowDefinition.getTasks(),
            tasks -> objectNode.set("tasks", toArrayNode(codeWorkflowContainerUuid, workflowDefinition, tasks, type)));

        return objectMapper.writeValueAsString(objectNode);
    }
}
