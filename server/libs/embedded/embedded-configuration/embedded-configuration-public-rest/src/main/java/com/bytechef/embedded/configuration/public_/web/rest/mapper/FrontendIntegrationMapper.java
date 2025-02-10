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

package com.bytechef.embedded.configuration.public_.web.rest.mapper;

import com.bytechef.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.embedded.configuration.public_.web.rest.mapper.config.EmbeddedConfigurationPublicMapperSpringConfig;
import com.bytechef.embedded.configuration.public_.web.rest.model.IntegrationModel;
import com.bytechef.embedded.configuration.public_.web.rest.model.WorkflowModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

@Mapper(
    config = EmbeddedConfigurationPublicMapperSpringConfig.class, implementationName = "EmbeddedPublic<CLASS_NAME>Impl")
public interface FrontendIntegrationMapper extends Converter<IntegrationInstanceConfigurationDTO, IntegrationModel> {

    @Override
    @Mapping(target = "allowMultipleInstances", source = "integration.allowMultipleInstances")
    @Mapping(target = "componentName", source = "integration.componentName")
    @Mapping(target = "description", source = "integration.description")
    @Mapping(target = "icon", source = "integration.icon")
    @Mapping(target = "title", source = "integration.title")
    @Mapping(target = "workflows", source = "integrationInstanceConfigurationWorkflows")
    IntegrationModel convert(IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO);

    @Mapping(target = "label", source = "workflow.label")
    @Mapping(target = "description", source = "workflow.description")
    WorkflowModel mapToWorkflowModel(
        IntegrationInstanceConfigurationWorkflowDTO integrationInstanceConfigurationWorkflowDTO);
}
