/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.codeworkflow.configuration.facade;

import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer;
import com.bytechef.platform.codeworkflow.configuration.domain.CodeWorkflowContainer.Language;
import com.bytechef.platform.codeworkflow.configuration.service.CodeWorkflowContainerService;
import com.bytechef.platform.codeworkflow.file.storage.CodeWorkflowFileStorage;
import com.bytechef.platform.constant.AppType;
import com.bytechef.workflow.definition.TaskDefinition;
import com.bytechef.workflow.definition.WorkflowDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Ivica Cardic
 */
@Service
@Transactional
public class CodeWorkflowContainerFacadeImpl implements CodeWorkflowContainerFacade {

    private final CodeWorkflowContainerService codeWorkflowContainerService;
    private final CodeWorkflowFileStorage codeWorkflowFileStorage;
    private final ObjectMapper objectMapper;

    @SuppressFBWarnings("EI")
    public CodeWorkflowContainerFacadeImpl(
        CodeWorkflowContainerService codeWorkflowContainerService, CodeWorkflowFileStorage codeWorkflowFileStorage,
        ObjectMapper objectMapper) {

        this.codeWorkflowContainerService = codeWorkflowContainerService;
        this.codeWorkflowFileStorage = codeWorkflowFileStorage;
        this.objectMapper = objectMapper;
    }

    @Override
    public CodeWorkflowContainer create(
        String name, String externalVersion, List<WorkflowDefinition> workflowDefinitions, Language language,
        byte[] bytes, AppType type) {

        try {
            CodeWorkflowContainer codeWorkflowContainer = new CodeWorkflowContainer();

            String codeWorkflowContainerReference = String.valueOf(UUID.randomUUID());

            codeWorkflowContainer.setCodeWorkflowContainerReference(codeWorkflowContainerReference);

            for (WorkflowDefinition workflowDefinition : workflowDefinitions) {
                String workflowId = String.valueOf(UUID.randomUUID());

                codeWorkflowContainer.addCodeWorkflow(
                    workflowId, workflowDefinition.getName(), OptionalUtils.orElse(workflowDefinition.getLabel(), null),
                    OptionalUtils.orElse(workflowDefinition.getDescription(), null),
                    codeWorkflowFileStorage.storeCodeWorkflowDefinition(
                        workflowId + ".json", getDefinition(codeWorkflowContainerReference, workflowDefinition, type)));
            }

            codeWorkflowContainer.setExternalVersion(externalVersion);
            codeWorkflowContainer.setLanguage(language);
            codeWorkflowContainer.setName(name);

            FileEntry workflowsFileEntry = codeWorkflowFileStorage.storeCodeWorkflowFile(
                codeWorkflowContainerReference + "." + language.getExtension(), bytes);

            codeWorkflowContainer.setWorkflowsFile(workflowsFileEntry);

            return codeWorkflowContainerService.create(codeWorkflowContainer);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayNode toArrayNode(
        String codeWorkflowContainerReference, WorkflowDefinition workflowDefinition,
        List<? extends TaskDefinition> tasks, AppType type) {

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
                            .put("codeWorkflowContainerReference", codeWorkflowContainerReference)
                            .put("workflowName", workflowDefinition.getName())
                            .put("taskName", taskDefinition.getName())
                            .put("type", type.ordinal())));
        }

        return arrayNode;
    }

    private String getDefinition(
        String codeWorkflowContainerReference, WorkflowDefinition workflowDefinition, AppType type) {

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
            tasks -> objectNode.set(
                "tasks", toArrayNode(codeWorkflowContainerReference, workflowDefinition, tasks, type)));

        try {
            return objectMapper.writeValueAsString(objectNode);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
