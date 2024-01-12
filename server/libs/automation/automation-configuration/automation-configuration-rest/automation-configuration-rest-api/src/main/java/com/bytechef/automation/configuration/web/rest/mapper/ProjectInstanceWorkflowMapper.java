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

package com.bytechef.automation.configuration.web.rest.mapper;

import com.bytechef.automation.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.automation.configuration.dto.ProjectInstanceWorkflowDTO;
import com.bytechef.automation.configuration.web.rest.mapper.config.AutomationConfigurationMapperSpringConfig;
import com.bytechef.automation.configuration.web.rest.model.ProjectInstanceWorkflowModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ProjectInstanceWorkflowMapper {

    @Mapper(config = AutomationConfigurationMapperSpringConfig.class)
    public interface ProjectInstanceWorkflowToProjectInstanceWorkflowModelMapper
        extends Converter<ProjectInstanceWorkflow, ProjectInstanceWorkflowModel> {

        @Override
        @Mapping(target = "lastExecutionDate", ignore = true)
        ProjectInstanceWorkflowModel convert(ProjectInstanceWorkflow projectInstanceWorkflow);
    }

    @Mapper(config = AutomationConfigurationMapperSpringConfig.class)
    public interface ProjectInstanceWorkflowDTOToProjectInstanceWorkflowModelMapper
        extends Converter<ProjectInstanceWorkflowDTO, ProjectInstanceWorkflowModel> {

        @Override
        ProjectInstanceWorkflowModel convert(ProjectInstanceWorkflowDTO projectInstanceWorkflow);
    }
}
