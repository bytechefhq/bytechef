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

package com.bytechef.hermes.integration.web.rest.mapper;

import com.bytechef.hermes.integration.domain.Integration;
import com.bytechef.hermes.integration.domain.IntegrationWorkflow;
import com.bytechef.hermes.integration.web.rest.mapper.config.IntegrationMapperSpringConfig;
import com.bytechef.hermes.integration.web.rest.model.IntegrationModel;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
@Mapper(config = IntegrationMapperSpringConfig.class)
public interface IntegrationModelMapper extends Converter<IntegrationModel, Integration> {

    @Mapping(target = "integrationWorkflows", source = "workflowIds")
    Integration convert(IntegrationModel integrationModel);

    default Set<IntegrationWorkflow> map(List<String> workflowIds) {
        if (workflowIds == null) {
            return Collections.emptySet();
        }

        return workflowIds.stream().map(IntegrationWorkflow::new).collect(Collectors.toSet());
    }
}
