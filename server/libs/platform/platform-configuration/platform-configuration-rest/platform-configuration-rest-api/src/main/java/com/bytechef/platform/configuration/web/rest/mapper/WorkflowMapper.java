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
import com.bytechef.platform.configuration.dto.WorkflowDTO;
import com.bytechef.platform.configuration.facade.WorkflowConnectionFacade;
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import com.bytechef.platform.configuration.web.rest.mapper.util.WorkflowMapperUtils;
import com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModel;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public abstract class WorkflowMapper {

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class)
    public abstract static class WorkflowToWorkflowModelMapper implements Converter<Workflow, WorkflowModel> {

        @Override
        @Mapping(target = "connectionsCount", ignore = true)
        @Mapping(target = "inputsCount", ignore = true)
        @Mapping(target = "triggers", ignore = true)
        @Mapping(target = "workflowTaskComponentNames", ignore = true)
        @Mapping(target = "workflowTriggerComponentNames", ignore = true)
        public abstract WorkflowModel convert(Workflow workflow);
    }

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class)
    public abstract static class WorkflowDTOToWorkflowModelMapper implements Converter<WorkflowDTO, WorkflowModel> {

        @Autowired
        private WorkflowConnectionFacade workflowConnectionFacade;

        @Override
        @Mapping(target = "connectionsCount", ignore = true)
        @Mapping(target = "inputsCount", ignore = true)
        @Mapping(target = "workflowTaskComponentNames", ignore = true)
        @Mapping(target = "workflowTriggerComponentNames", ignore = true)
        public abstract WorkflowModel convert(WorkflowDTO workflowDTO);

        @AfterMapping
        public void afterMapping(Workflow workflow, @MappingTarget WorkflowModel workflowModel) {
            WorkflowMapperUtils.afterMapping(workflow, workflowModel, workflowConnectionFacade);
        }
    }

    @Mapper(config = PlatformConfigurationMapperSpringConfig.class)
    public abstract static class WorkflowModelToWorkflowBasicModel implements Converter<Workflow, WorkflowBasicModel> {

        @Override
        public abstract WorkflowBasicModel convert(Workflow source);
    }
}
