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

package com.bytechef.embedded.configuration.web.rest.mapper;

import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationBasicModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class IntegrationInstanceConfigurationMapper {

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceConfigurationBasicToIntegrationInstanceModelMapper
        extends Converter<IntegrationInstanceConfiguration, IntegrationInstanceConfigurationBasicModel> {

        @Mapping(target = "lastExecutionDate", ignore = true)
        @Override
        IntegrationInstanceConfigurationBasicModel convert(IntegrationInstanceConfiguration integrationInstance);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceConfigurationToIntegrationInstanceModelMapper
        extends Converter<IntegrationInstanceConfiguration, IntegrationInstanceConfigurationModel> {

        @Mapping(target = "integration", ignore = true)
        @Mapping(target = "integrationInstanceConfigurationWorkflows", ignore = true)
        @Mapping(target = "lastExecutionDate", ignore = true)
        @Mapping(target = "tags", ignore = true)
        @Override
        IntegrationInstanceConfigurationModel convert(IntegrationInstanceConfiguration integrationInstance);

    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceConfigurationDTOToIntegrationInstanceModelMapper
        extends Converter<IntegrationInstanceConfigurationDTO, IntegrationInstanceConfigurationModel> {

        @Override
        @Mapping(target = "integration.integrationVersion", source = "integration.lastVersion")
        @Mapping(target = "integration.publishedDate", source = "integration.lastPublishedDate")
        @Mapping(target = "integration.status", source = "integration.lastStatus")
        IntegrationInstanceConfigurationModel convert(
            IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO);
    }
}
