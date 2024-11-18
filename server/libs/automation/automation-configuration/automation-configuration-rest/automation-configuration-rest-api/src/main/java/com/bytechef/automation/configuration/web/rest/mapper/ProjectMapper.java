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

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.dto.ProjectDTO;
import com.bytechef.automation.configuration.web.rest.mapper.config.ProjectConfigurationMapperSpringConfig;
import com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectModel;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class ProjectMapper {

    @Mapper(config = ProjectConfigurationMapperSpringConfig.class)
    public interface ProjectToProjectBasicModelMapper extends Converter<Project, ProjectBasicModel> {

        @Override
        ProjectBasicModel convert(Project project);
    }

    @Mapper(config = ProjectConfigurationMapperSpringConfig.class)
    public interface ProjectDTOToProjectModelMapper extends Converter<ProjectDTO, ProjectModel> {

        @Override
        ProjectModel convert(ProjectDTO projectDTO);

        @InheritInverseConfiguration
        @DelegatingConverter
        @Mapping(target = "projectVersions", ignore = true)
        ProjectDTO invertConvert(ProjectModel projectModel);
    }
}
