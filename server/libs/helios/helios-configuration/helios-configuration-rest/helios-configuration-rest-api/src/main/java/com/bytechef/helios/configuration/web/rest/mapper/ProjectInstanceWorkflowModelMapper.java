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

package com.bytechef.helios.configuration.web.rest.mapper;

import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflow;
import com.bytechef.helios.configuration.domain.ProjectInstanceWorkflowConnection;
import com.bytechef.helios.configuration.web.rest.mapper.config.ProjectConfigurationMapperSpringConfig;
import com.bytechef.helios.configuration.web.rest.model.ProjectInstanceWorkflowConnectionModel;
import com.bytechef.helios.configuration.web.rest.model.ProjectInstanceWorkflowModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ProjectConfigurationMapperSpringConfig.class)
public interface ProjectInstanceWorkflowModelMapper
    extends Converter<ProjectInstanceWorkflowModel, ProjectInstanceWorkflow> {

    @Override
    ProjectInstanceWorkflow convert(ProjectInstanceWorkflowModel projectInstanceWorkflowModel);

    ProjectInstanceWorkflowConnection map(ProjectInstanceWorkflowConnectionModel value);
}
