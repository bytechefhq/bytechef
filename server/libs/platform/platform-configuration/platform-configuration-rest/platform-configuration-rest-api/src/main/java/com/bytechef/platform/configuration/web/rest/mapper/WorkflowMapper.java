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

package com.bytechef.platform.configuration.web.rest.mapper;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModel;
import com.bytechef.platform.definition.WorkflowNodeType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public abstract class WorkflowMapper {

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class)
    public abstract static class WorkflowToWorkflowModelMapper implements Converter<WorkflowDTO, WorkflowModel> {

        @Override
        public abstract WorkflowModel convert(WorkflowDTO workflow);
    }

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class)
    public static abstract class WorkflowModelToWorkflowBasicModel implements Converter<Workflow, WorkflowBasicModel> {

        @Override
        @Mapping(target = "manualTrigger", ignore = true)
        @Mapping(target = "workflowTaskComponentNames", ignore = true)
        @Mapping(target = "workflowTriggerComponentNames", ignore = true)
        public abstract WorkflowBasicModel convert(Workflow source);

        @AfterMapping
        public void afterMapping(Workflow workflow, @MappingTarget WorkflowBasicModel workflowBasicModel) {
            workflowBasicModel.setManualTrigger(CollectionUtils.isEmpty(WorkflowTrigger.of(workflow)));
            workflowBasicModel.setWorkflowTaskComponentNames(getWorkflowTaskComponentNames(workflow.getTasks()));

            List<String> workflowTriggerComponentNames = WorkflowTrigger
                .of(workflow)
                .stream()
                .map(workflowTrigger -> WorkflowNodeType.ofType(workflowTrigger.getType()))
                .map(WorkflowNodeType::componentName)
                .toList();

            workflowBasicModel.setWorkflowTriggerComponentNames(
                workflowTriggerComponentNames.isEmpty() ? List.of("manual") : workflowTriggerComponentNames);
        }

        private List<String> getWorkflowTaskComponentNames(List<WorkflowTask> workflowTasks) {
            List<String> workflowTaskComponentNames = new ArrayList<>();

            for (WorkflowTask workflowTask : workflowTasks) {
                workflowTaskComponentNames.add(
                    WorkflowNodeType
                        .ofType(workflowTask.getType())
                        .componentName());

                Map<String, ?> parameters = workflowTask.getParameters();

                for (Map.Entry<String, ?> entry : parameters.entrySet()) {
                    if (entry.getValue() instanceof WorkflowTask curWorkflowTask) {
                        workflowTaskComponentNames.addAll(getWorkflowTaskComponentNames(List.of(curWorkflowTask)));
                    } else if (entry.getValue() instanceof List<?> curList) {
                        if (!curList.isEmpty() && curList.getFirst() instanceof WorkflowTask) {
                            for (Object item : curList) {
                                workflowTaskComponentNames
                                    .addAll(getWorkflowTaskComponentNames(List.of((WorkflowTask) item)));
                            }
                        }
                    } else if (entry.getValue() instanceof Map<?, ?> curMap) {
                        for (Map.Entry<?, ?> curMapEntry : curMap.entrySet()) {
                            if (curMapEntry.getValue() instanceof WorkflowTask curWorkflowTask) {
                                workflowTaskComponentNames
                                    .addAll(getWorkflowTaskComponentNames(List.of(curWorkflowTask)));
                            }
                        }
                    }
                }
            }

            return workflowTaskComponentNames;
        }
    }
}
