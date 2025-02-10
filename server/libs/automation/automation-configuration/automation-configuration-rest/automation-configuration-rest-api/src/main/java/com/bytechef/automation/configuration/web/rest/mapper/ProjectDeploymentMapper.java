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

import com.bytechef.automation.configuration.domain.ProjectDeployment;
import com.bytechef.automation.configuration.dto.ProjectDeploymentDTO;
import com.bytechef.automation.configuration.web.rest.mapper.config.AutomationConfigurationMapperSpringConfig;
import com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentBasicModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectDeploymentModel;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ProjectDeploymentMapper {

    @Mapper(config = AutomationConfigurationMapperSpringConfig.class)
    public interface ProjectDeploymentBasicToProjectDeploymentModelMapper
        extends Converter<ProjectDeployment, ProjectDeploymentBasicModel> {

        @Mapping(target = "lastExecutionDate", ignore = true)
        @Override
        ProjectDeploymentBasicModel convert(ProjectDeployment projectInstanc);
    }

    @Mapper(config = AutomationConfigurationMapperSpringConfig.class)
    public interface ProjectDeploymentToProjectDeploymentModelMapper
        extends Converter<ProjectDeployment, ProjectDeploymentModel> {

        @Mapping(target = "lastExecutionDate", ignore = true)
        @Mapping(target = "project", ignore = true)
        @Mapping(target = "projectDeploymentWorkflows", ignore = true)
        @Mapping(target = "tags", ignore = true)
        @Override
        ProjectDeploymentModel convert(ProjectDeployment projectDeployment);
    }

    @Mapper(config = AutomationConfigurationMapperSpringConfig.class)
    public interface ProjectDeploymentDTOToProjectDeploymentModelMapper
        extends Converter<ProjectDeploymentDTO, ProjectDeploymentModel> {

        @Override
        ProjectDeploymentModel convert(ProjectDeploymentDTO projectDeploymentDTO);

        @InheritInverseConfiguration
        @DelegatingConverter
        @Mapping(target = "project", ignore = true)
        ProjectDeploymentDTO invertConvert(ProjectDeploymentModel projectDeploymentModel);
    }
}
