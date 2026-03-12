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

package com.bytechef.platform.workflow.validator;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.component.domain.ActionDefinition;
import com.bytechef.platform.component.domain.ArrayProperty;
import com.bytechef.platform.component.domain.FileEntryProperty;
import com.bytechef.platform.component.domain.ObjectProperty;
import com.bytechef.platform.component.domain.Property;
import com.bytechef.platform.component.domain.TriggerDefinition;
import com.bytechef.platform.component.service.ActionDefinitionService;
import com.bytechef.platform.component.service.TriggerDefinitionService;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.domain.BaseProperty;
import com.bytechef.platform.domain.OutputResponse;
import com.bytechef.platform.workflow.task.dispatcher.domain.TaskDispatcherDefinition;
import com.bytechef.platform.workflow.task.dispatcher.service.TaskDispatcherDefinitionService;
import com.bytechef.platform.workflow.validator.model.PropertyInfo;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * @author Marko Kriskovic
 */
@Service
public class WorkflowValidatorFacadeImpl implements WorkflowValidatorFacade {

    private final ActionDefinitionService actionDefinitionService;
    private final TaskDispatcherDefinitionService taskDispatcherDefinitionService;
    private final TriggerDefinitionService triggerDefinitionService;

    public WorkflowValidatorFacadeImpl(
        ActionDefinitionService actionDefinitionService,
        TaskDispatcherDefinitionService taskDispatcherDefinitionService,
        TriggerDefinitionService triggerDefinitionService) {

        this.actionDefinitionService = actionDefinitionService;
        this.taskDispatcherDefinitionService = taskDispatcherDefinitionService;
        this.triggerDefinitionService = triggerDefinitionService;
    }

    @Override
    public WorkflowValidationResult validateWorkflow(String workflow) {
        StringBuilder errors = new StringBuilder();
        StringBuilder warnings = new StringBuilder();

        WorkflowValidator.validateWorkflow(
            workflow, this::getTaskProperties, this::getTaskOutputProperty, new HashMap<>(), new HashMap<>(),
            errors, warnings);

        String errorsString = errors.toString();

        List<String> errorList = Arrays.stream(errorsString.split("\n"))
            .filter(line -> !line.isBlank())
            .toList();

        String warningsString = warnings.toString();

        List<String> warningList = Arrays.stream(warningsString.split("\n"))
            .filter(line -> !line.isBlank())
            .toList();

        return new WorkflowValidationResult(errorList, warningList);
    }

    private List<PropertyInfo> getTaskProperties(String taskType, String kind) {
        try {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(taskType);

            if ("trigger".equals(kind)) {
                TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                    workflowNodeType.name(), workflowNodeType.version(),
                    Objects.requireNonNull(workflowNodeType.operation()));

                return triggerDefinition.getProperties()
                    .stream()
                    .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                    .toList();
            } else if (workflowNodeType.operation() != null) {
                ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                    workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());

                return actionDefinition.getProperties()
                    .stream()
                    .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                    .toList();
            } else {
                TaskDispatcherDefinition taskDispatcherDefinition =
                    taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                        workflowNodeType.name(), workflowNodeType.version());

                return Stream.concat(
                    CollectionUtils.stream(taskDispatcherDefinition.getProperties()),
                    CollectionUtils.stream(taskDispatcherDefinition.getTaskProperties()))
                    .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                    .toList();
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    @Nullable
    private PropertyInfo getTaskOutputProperty(String taskType, String kind, StringBuilder warnings) {
        try {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(taskType);

            OutputResponse outputResponse;

            if ("trigger".equals(kind)) {
                TriggerDefinition triggerDefinition = triggerDefinitionService.getTriggerDefinition(
                    workflowNodeType.name(), workflowNodeType.version(),
                    Objects.requireNonNull(workflowNodeType.operation()));

                outputResponse = triggerDefinition.getOutputResponse();
            } else if (workflowNodeType.operation() != null) {
                ActionDefinition actionDefinition = actionDefinitionService.getActionDefinition(
                    workflowNodeType.name(), workflowNodeType.version(), workflowNodeType.operation());

                outputResponse = actionDefinition.getOutputResponse();
            } else {
                TaskDispatcherDefinition taskDispatcherDefinition =
                    taskDispatcherDefinitionService.getTaskDispatcherDefinition(
                        workflowNodeType.name(), workflowNodeType.version());

                outputResponse = taskDispatcherDefinition.getOutputResponse();
            }

            if (outputResponse != null && outputResponse.outputSchema() != null) {
                return toPropertyInfo(outputResponse.outputSchema());
            }

            return null;
        } catch (Exception e) {
            warnings.append("Could not retrieve output for task type: ")
                .append(taskType)
                .append("\n");

            return null;
        }
    }

    private static PropertyInfo toPropertyInfo(BaseProperty baseProperty) {
        String type;
        List<PropertyInfo> nestedPropertyInfos = null;

        switch (baseProperty) {
            case ObjectProperty objectProperty -> {
                type = "OBJECT";
                List<? extends Property> properties = objectProperty.getProperties();

                if (properties != null && !properties.isEmpty()) {
                    nestedPropertyInfos = properties.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case ArrayProperty arrayProperty -> {
                type = "ARRAY";
                List<? extends Property> items = arrayProperty.getItems();

                if (items != null && !items.isEmpty()) {
                    nestedPropertyInfos = items.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case FileEntryProperty fileEntryProperty -> {
                type = "FILE_ENTRY";
                List<? extends Property> properties = fileEntryProperty.getProperties();

                if (properties != null && !properties.isEmpty()) {
                    nestedPropertyInfos = properties.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case Property property -> {
                com.bytechef.component.definition.Property.Type propertyType = property.getType();

                type = propertyType.name();
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.ObjectProperty objectProperty -> {
                type = "OBJECT";
                List<? extends com.bytechef.platform.workflow.task.dispatcher.domain.Property> properties =
                    objectProperty
                        .getProperties();

                if (properties != null && !properties.isEmpty()) {
                    nestedPropertyInfos = properties.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.ArrayProperty arrayProperty -> {
                type = "ARRAY";
                List<? extends com.bytechef.platform.workflow.task.dispatcher.domain.Property> items = arrayProperty
                    .getItems();

                if (items != null && !items.isEmpty()) {
                    nestedPropertyInfos = items.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.FileEntryProperty fileEntryProperty -> {
                type = "FILE_ENTRY";
                List<? extends com.bytechef.platform.workflow.task.dispatcher.domain.ValueProperty<?>> properties =
                    fileEntryProperty.getProperties();

                if (properties != null && !properties.isEmpty()) {
                    nestedPropertyInfos = properties.stream()
                        .map(WorkflowValidatorFacadeImpl::toPropertyInfo)
                        .toList();
                }
            }
            case com.bytechef.platform.workflow.task.dispatcher.domain.Property property -> {
                com.bytechef.platform.workflow.task.dispatcher.definition.Property.Type propertyType =
                    property.getType();

                type = propertyType.name();
            }
            default -> type = "OBJECT";
        }

        return new PropertyInfo(
            baseProperty.getName(), type, baseProperty.getDescription(), baseProperty.getRequired(),
            baseProperty.getExpressionEnabled(), baseProperty.getDisplayCondition(), nestedPropertyInfos);
    }
}
