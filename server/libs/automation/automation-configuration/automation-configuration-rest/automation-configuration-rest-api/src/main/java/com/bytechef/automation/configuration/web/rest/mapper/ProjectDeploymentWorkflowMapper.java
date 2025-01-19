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

import com.bytechef.automation.configuration.domain.ProjectDeploymentWorkflow;
import com.bytechef.automation.configuration.dto.ProjectDeploymentWorkflowDTO;
import com.bytechef.automation.configuration.web.rest.mapper.config.ProjectConfigurationMapperSpringConfig;
import com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentWorkflowModel;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ProjectDeploymentWorkflowMapper {

    @Mapper(config = ProjectConfigurationMapperSpringConfig.class)
    public interface ProjectDeploymentWorkflowToProjectDeploymentWorkflowModelMapper
        extends Converter<ProjectDeploymentWorkflow, ProjectDeploymentWorkflowModel> {

        @Override
        @Mapping(target = "lastExecutionDate", ignore = true)
        @Mapping(target = "staticWebhookUrl", ignore = true)
        @Mapping(target = "workflowReferenceCode", ignore = true)
        ProjectDeploymentWorkflowModel convert(ProjectDeploymentWorkflow projectDeploymentWorkflow);

        @InheritInverseConfiguration
        @DelegatingConverter
        ProjectDeploymentWorkflow invertConvert(ProjectDeploymentWorkflowModel projectDeploymentWorkflowModel);
    }

    @Mapper(config = ProjectConfigurationMapperSpringConfig.class)
    public interface ProjectDeploymentWorkflowDTOToProjectDeploymentWorkflowModelMapper
        extends Converter<ProjectDeploymentWorkflowDTO, ProjectDeploymentWorkflowModel> {

        @Override
        ProjectDeploymentWorkflowModel convert(ProjectDeploymentWorkflowDTO projectDeploymentWorkflowDTO);

        @InheritInverseConfiguration
        @DelegatingConverter
        ProjectDeploymentWorkflowDTO invertConvert(ProjectDeploymentWorkflowModel projectDeploymentWorkflowModel);
    }
}
