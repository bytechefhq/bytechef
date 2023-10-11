
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

package com.bytechef.atlas.configuration.workflow.mapper;

import com.bytechef.atlas.configuration.workflow.contributor.WorkflowReservedWordContributor;
import com.bytechef.atlas.configuration.constant.WorkflowConstants;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.FileCopyUtils;
import com.bytechef.commons.util.JsonUtils;
import com.bytechef.commons.util.LocalDateTimeUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

/**
 * @author Matija Petanjek
 */
abstract class AbstractWorkflowMapper implements WorkflowMapper {

    private static final Logger logger = LoggerFactory.getLogger(AbstractWorkflowMapper.class);

    static List<String> additionalWorkflowReservedWords = new ArrayList<>();

    static {
        try {
            ServiceLoader<WorkflowReservedWordContributor> serviceLoader = ServiceLoader.load(
                WorkflowReservedWordContributor.class);

            for (WorkflowReservedWordContributor workflowReservedWordContributor : serviceLoader) {
                additionalWorkflowReservedWords.addAll(workflowReservedWordContributor.getReservedWords());
            }
        } catch (ServiceConfigurationError e) {
            if (logger.isDebugEnabled()) {
                logger.debug(e.getMessage(), e);
            }
        }
    }

    private final Workflow.Format format;
    private final ObjectMapper objectMapper;

    AbstractWorkflowMapper(Workflow.Format format, ObjectMapper objectMapper) {
        this.format = format;
        this.objectMapper = objectMapper;
    }

    @Override
    public Workflow readWorkflow(WorkflowResource workflowResource, int type) {
        return readWorkflow(workflowResource, type, objectMapper);
    }

    @Override
    public Map<String, Object> readWorkflowMap(WorkflowResource workflowResource) {
        return readWorkflowMap(workflowResource, objectMapper);
    }

    @Override
    public WorkflowMapper resolve(WorkflowResource workflowResource) {
        return workflowResource.getWorkflowFormat() == format ? this : null;
    }

    protected Workflow readWorkflow(WorkflowResource workflowResource, int type, ObjectMapper objectMapper) {
        try {
            String definition = readDefinition(workflowResource);

            Map<String, Object> workflowMap = parse(definition, objectMapper);

            return new Workflow(
                workflowResource.getId(), definition, workflowResource.getWorkflowFormat(),
                LocalDateTimeUtils.getLocalDateTime(new Date(workflowResource.lastModified())), workflowMap,
                workflowResource.getMetadata(), type);
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

    private Map<String, Object> parse(String workflow, ObjectMapper objectMapper) {
        Map<String, Object> workflowMap = JsonUtils.read(workflow, new TypeReference<>() {}, objectMapper);

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

            Validate.notNull(output.get(WorkflowConstants.NAME), "output definition must specify a 'name'");
            Validate.notNull(output.get(WorkflowConstants.VALUE), "output definition must specify a 'value'");
        }
    }

    @SuppressWarnings("unchecked")
    private void validateReservedWords(Map<String, Object> workflowMap) {
        for (Map.Entry<String, Object> entry : workflowMap.entrySet()) {
            String k = entry.getKey();
            Object v = entry.getValue();

            Validate.isTrue(
                CollectionUtils.contains(
                    CollectionUtils.concat(
                        WorkflowConstants.WORKFLOW_DEFINITION_CONSTANTS, additionalWorkflowReservedWords),
                    k),
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
