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

package com.bytechef.atlas.repository.workflow.mapper;

import com.bytechef.atlas.constants.WorkflowConstants;
import com.bytechef.atlas.domain.Workflow;
import com.bytechef.atlas.error.ExecutionError;
import com.bytechef.atlas.workflow.WorkflowResource;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * @author Matija Petanjek
 */
public abstract class AbstractWorkflowMapper implements WorkflowMapper {

    public Workflow readValue(WorkflowResource workflowResource, ObjectMapper objectMapper) {
        try {
            Map<String, Object> jsonMap = parse(workflowResource, objectMapper);

            jsonMap.put(WorkflowConstants.ID, workflowResource.getId());

            return new Workflow(jsonMap);
        } catch (Exception e) {
            Workflow workflow = new Workflow(Collections.singletonMap(WorkflowConstants.ID, workflowResource.getId()));

            workflow.setError(new ExecutionError(e.getMessage(), Arrays.asList(ExceptionUtils.getStackFrames(e))));

            return workflow;
        }
    }

    private Map<String, Object> parse(Resource resource, ObjectMapper objectMapper) {
        try (InputStream in = resource.getInputStream()) {
            String workflow = IOUtils.toString(in, StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> workflowMap = objectMapper.readValue(workflow, Map.class);

            validate(workflowMap);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawTasks = (List<Map<String, Object>>) workflowMap.get(WorkflowConstants.TASKS);

            Assert.notNull(rawTasks, "no tasks found");
            Assert.notEmpty(rawTasks, "no tasks found");

            List<Map<String, Object>> tasks = new ArrayList<>();

            for (int i = 0; i < rawTasks.size(); i++) {
                Map<String, Object> rawTask = rawTasks.get(i);

                rawTask.put(WorkflowConstants.TASK_NUMBER, i + 1);

                tasks.add(rawTask);
            }

            workflowMap.put(WorkflowConstants.TASKS, tasks);

            return workflowMap;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void validate(Map<String, Object> aMap) {
        validateReservedWords(aMap);
        validateOutputs(aMap);
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
        List<String> reservedWords = Arrays.asList(WorkflowConstants.RESERVED_WORDS);

        for (Map.Entry<String, Object> entry : workflowMap.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();

            Assert.isTrue(!reservedWords.contains(k), "reserved word: " + k);

            if (v instanceof Map) {
                validate((Map<String, Object>) v);
            }

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
