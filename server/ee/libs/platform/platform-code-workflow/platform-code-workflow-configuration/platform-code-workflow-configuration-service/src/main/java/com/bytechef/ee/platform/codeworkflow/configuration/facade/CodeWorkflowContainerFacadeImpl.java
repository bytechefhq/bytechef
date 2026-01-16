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
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
    private final ObjectMapper objectMapper;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public CodeWorkflowContainerFacadeImpl(
        CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
        ObjectMapper objectMapper, WorkflowService workflowService) {

        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.objectMapper = objectMapper;
        this.workflowService = workflowService;
    }

    @Override
    public CodeWorkflowContainer create(
        String name, String externalVersion, List<WorkflowDefinition> workflowDefinitions, Language language,
        byte[] bytes, PlatformType type) {

        try {
            UUID codeWorkflowContainerId = UUID.randomUUID();

            CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer(codeWorkflowContainerId);

            for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
                String definition = getDefinition(String.valueOf(codeWorkflowContainerId), workflowDefinition, type);

                Workflow workflow = workflowService.create(definition, Workflow.Format.JSON, Workflow.SourceType.JDBC);

                codeWorkflowContainer.addCodeWorkflow(
                    UUID.fromString(Objects.requireNonNull(workflow.getId())), workflowDefinition.getName());
            }

            codeWorkflowContainer.setExternalVersion(externalVersion);
            codeWorkflowContainer.setLanguage(language);
            codeWorkflowContainer.setName(name);

            FileEntry workflowsFileEntry = codeWorkflowFileStorage.storeCodeWorkflowFile(
                codeWorkflowContainerId + "." + language.getExtension(), bytes);

            codeWorkflowContainer.setWorkflows(workflowsFileEntry);

            return codeWorkflowContainerService.create(codeWorkflowContainer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayNode toArrayNode(
        String codeWorkflowContainerUuid, WorkflowDefinition workflowDefinition, List<? extends TaskDefinition> tasks,
        PlatformType type) {

        ArrayNode arrayNode = objectMapper.createArrayNode();

        for (TaskDefinition taskDefinition : tasks) {
            arrayNode.add(
                objectMapper.createObjectNode()
                    .put("description", OptionalUtils.orElse(taskDefinition.getDescription(), null))
                    .put("label", OptionalUtils.orElse(taskDefinition.getLabel(), null))
                    .put("name", taskDefinition.getName())
                    .put("type", "codeWorkflow/v1/perform")
                    // TODO taskDefinition.getParameters()
                    .set(
                        "parameters",
                        objectMapper.createObjectNode()
                            .put("codeWorkflowContainerUuid", codeWorkflowContainerUuid)
                            .put("workflowName", workflowDefinition.getName())
                            .put("taskName", taskDefinition.getName())
                            .put("type", type.ordinal())));
        }

        return arrayNode;
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
