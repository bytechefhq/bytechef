
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.atlas.workflow.mapper;

import com.bytechef.atlas.constant.WorkflowConstants;
import com.bytechef.atlas.domain.Workflow;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * @author Matija Petanjek
 */
abstract class AbstractWorkflowMapper implements WorkflowMapper {

    protected Workflow readWorkflow(WorkflowResource workflowResource, ObjectMapper objectMapper) {
        try {
            String definition = readDefinition(workflowResource);

            Map<String, Object> workflowMap = parse(definition, objectMapper);

            return new Workflow(
                definition, workflowResource.getWorkflowFormat(), workflowResource.getId(), workflowMap);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Map<String, Object> readWorkflowMap(WorkflowResource workflowResource, ObjectMapper objectMapper) {
        try {
            String definition = readDefinition(workflowResource);

            return parse(definition, objectMapper);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> parse(String workflow, ObjectMapper objectMapper) throws JsonProcessingException {
        @SuppressWarnings("unchecked")
        Map<String, Object> workflowMap = objectMapper.readValue(workflow, Map.class);

        validate(workflowMap);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rawTasks = (List<Map<String, Object>>) workflowMap.get(WorkflowConstants.TASKS);

        if (rawTasks == null) {
            rawTasks = Collections.emptyList();
        }

        List<Map<String, Object>> tasks = new ArrayList<>();

        for (int i = 0; i < rawTasks.size(); i++) {
            Map<String, Object> rawTask = rawTasks.get(i);

            rawTask.put(WorkflowConstants.TASK_NUMBER, i + 1);

            tasks.add(rawTask);
        }

        workflowMap.put(WorkflowConstants.TASKS, tasks);

        return workflowMap;
    }

    private String readDefinition(Resource resource) throws IOException {
        try (InputStream in = resource.getInputStream()) {
            return FileCopyUtils.copyToString(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    private void validate(Map<String, Object> map) {
        validateReservedWords(map);
        validateOutputs(map);
    }

    @SuppressWarnings("unchecked")
    private void validateOutputs(Map<String, Object> workflow) {
        List<Map<String, Object>> outputs = (List<Map<String, Object>>) workflow.get(WorkflowConstants.OUTPUTS);

        for (int i = 0; outputs != null && i < outputs.size(); i++) {
            Map<String, Object> output = outputs.get(i);

            Assert.notNull(output.get(WorkflowConstants.NAME), "output definition must specify a 'name'");
            Assert.notNull(output.get(WorkflowConstants.VALUE), "output definition must specify a 'value'");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateReservedWords(Map<String, Object> workflowMap) {
        for (Map.Entry<String, Object> entry : workflowMap.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();

            Assert.isTrue(
                WorkflowConstants.WORKFLOW_DEFINITION_CONSTANTS.contains(k),
                "unknown workflow definition property: " + k);

            if (v instanceof List) {
                List<Object> items = (List<Object>) v;
                for (Object item : items) {
                    if (item instanceof Map) {
                        validate((Map<String, Object>) item);
                    }
                }
            }
        }
    }
}
