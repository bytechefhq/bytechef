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

import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.automation.configuration.dto.ProjectInstanceDTO;
import com.bytechef.automation.configuration.web.rest.mapper.config.ProjectConfigurationMapperSpringConfig;
import com.bytechef.automation.configuration.web.rest.model.ProjectInstanceBasicModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectInstanceModel;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ProjectInstanceMapper {

    @Mapper(config = ProjectConfigurationMapperSpringConfig.class)
    public interface ProjectInstanceBasicToProjectInstanceModelMapper
        extends Converter<ProjectInstance, ProjectInstanceBasicModel> {

        @Mapping(target = "lastExecutionDate", ignore = true)
        @Override
        ProjectInstanceBasicModel convert(ProjectInstance projectInstanc);
    }

    @Mapper(config = ProjectConfigurationMapperSpringConfig.class)
    public interface ProjectInstanceToProjectInstanceModelMapper
        extends Converter<ProjectInstance, ProjectInstanceModel> {

        @Mapping(target = "lastExecutionDate", ignore = true)
        @Mapping(target = "project", ignore = true)
        @Mapping(target = "projectInstanceWorkflows", ignore = true)
        @Mapping(target = "tags", ignore = true)
        @Override
        ProjectInstanceModel convert(ProjectInstance projectInstance);
    }

    @Mapper(config = ProjectConfigurationMapperSpringConfig.class)
    public interface ProjectInstanceDTOToProjectInstanceModelMapper
        extends Converter<ProjectInstanceDTO, ProjectInstanceModel> {

        @Override
        ProjectInstanceModel convert(ProjectInstanceDTO projectInstanceDTO);

        @InheritInverseConfiguration
        @DelegatingConverter
        @Mapping(target = "project", ignore = true)
        ProjectInstanceDTO invertConvert(ProjectInstanceModel projectInstanceModel);
    }
}
