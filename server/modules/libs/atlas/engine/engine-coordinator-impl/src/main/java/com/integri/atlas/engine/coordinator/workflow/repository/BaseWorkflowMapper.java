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

package com.integri.atlas.engine.coordinator.workflow.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integri.atlas.engine.coordinator.workflow.SimpleWorkflow;
import com.integri.atlas.engine.coordinator.workflow.Workflow;
import com.integri.atlas.engine.core.DSL;
import com.integri.atlas.engine.core.error.ErrorObject;
import com.integri.atlas.engine.core.task.SimpleWorkflowTask;
import com.integri.atlas.engine.core.task.Task;
import java.io.IOException;
import java.io.InputStream;
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
public abstract class BaseWorkflowMapper implements WorkflowMapper {

    public Workflow readValue(WorkflowResource aWorkflowResource, ObjectMapper objectMapper) {
        try {
            Map<String, Object> jsonMap = parse(aWorkflowResource, objectMapper);
            jsonMap.put(DSL.ID, aWorkflowResource.getId());
            return new SimpleWorkflow(jsonMap);
        } catch (Exception e) {
            SimpleWorkflow workflow = new SimpleWorkflow(Collections.singletonMap(DSL.ID, aWorkflowResource.getId()));
            workflow.setError(new ErrorObject(e.getMessage(), ExceptionUtils.getStackFrames(e)));
            return workflow;
        }
    }

    protected Map<String, Object> parse(Resource aResource, ObjectMapper objectMapper) {
        try (InputStream in = aResource.getInputStream()) {
            String workflow = IOUtils.toString(in);
            Map<String, Object> workflowMap = objectMapper.readValue(workflow, Map.class);
            validate(workflowMap);
            List<Map<String, Object>> rawTasks = (List<Map<String, Object>>) workflowMap.get(DSL.TASKS);
            Assert.notNull(rawTasks, "no tasks found");
            Assert.notEmpty(rawTasks, "no tasks found");
            List<Task> tasks = new ArrayList<>();
            for (int i = 0; i < rawTasks.size(); i++) {
                Map<String, Object> rt = rawTasks.get(i);
                SimpleWorkflowTask mutableTask = new SimpleWorkflowTask(rt);
                mutableTask.setTaskNumber(i + 1);
                tasks.add(mutableTask);
            }
            workflowMap.put(DSL.TASKS, tasks);
            return workflowMap;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void validate(Map<String, Object> aMap) {
        validateReservedWords(aMap);
        validateOutputs(aMap);
    }

    private void validateOutputs(Map<String, Object> aWorkflow) {
        List<Map<String, Object>> outputs = (List<Map<String, Object>>) aWorkflow.get(DSL.OUTPUTS);
        for (int i = 0; outputs != null && i < outputs.size(); i++) {
            Map<String, Object> output = outputs.get(i);
            Assert.notNull(output.get(DSL.NAME), "output definition must specify a 'name'");
            Assert.notNull(output.get(DSL.VALUE), "output definition must specify a 'value'");
        }
    }

    private void validateReservedWords(Map<String, Object> aWorkflow) {
        List<String> reservedWords = Arrays.asList(DSL.RESERVED_WORDS);
        for (Map.Entry<String, Object> entry : aWorkflow.entrySet()) {
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
