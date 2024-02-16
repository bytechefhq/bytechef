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
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import com.bytechef.platform.configuration.web.rest.model.WorkflowBasicModel;
import com.bytechef.platform.configuration.web.rest.model.WorkflowModel;
import org.mapstruct.Mapper;
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
    public interface WorkflowModelToWorkflowBasicModel extends Converter<Workflow, WorkflowBasicModel> {

        @Override
        WorkflowBasicModel convert(Workflow source);
    }
}
