
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

package com.bytechef.hermes.project.web.rest.mapper;

import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.web.rest.model.ConnectionModel;
import com.bytechef.hermes.project.domain.ProjectInstance;
import com.bytechef.hermes.project.web.rest.mapper.config.ProjectMapperSpringConfig;
import com.bytechef.hermes.project.web.rest.model.ProjectInstanceModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = ProjectMapperSpringConfig.class)
public interface ProjectInstanceMapper extends Converter<ProjectInstance, ProjectInstanceModel> {

    ProjectInstanceModel convert(ProjectInstance projectInstance);

    @Mapping(target = "active", ignore = true)
    @Mapping(target = "tags", ignore = true)
    ConnectionModel convert(Connection connection);
}
