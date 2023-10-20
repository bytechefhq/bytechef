
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

package com.bytechef.helios.project.web.rest.mapper;

import com.bytechef.helios.project.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.project.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.project.dto.ProjectInstanceDTO;
import com.bytechef.helios.project.web.rest.mapper.config.ProjectMapperSpringConfig;
import com.bytechef.helios.project.web.rest.model.ProjectInstanceModel;
import com.bytechef.helios.project.web.rest.model.ProjectInstanceWorkflowConnectionModel;
import com.bytechef.helios.project.web.rest.model.ProjectInstanceWorkflowModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ProjectMapperSpringConfig.class)
public interface ProjectInstanceModelMapper extends Converter<ProjectInstanceModel, ProjectInstanceDTO> {

    @Override
    @Mapping(target = "project", ignore = true)
    ProjectInstanceDTO convert(ProjectInstanceModel projectInstanceModel);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "projectInstanceId", ignore = true)
    ProjectInstanceWorkflow map(ProjectInstanceWorkflowModel projectInstanceWorkflowModel);

    ProjectInstanceWorkflowConnection map(ProjectInstanceWorkflowConnectionModel value);
}
