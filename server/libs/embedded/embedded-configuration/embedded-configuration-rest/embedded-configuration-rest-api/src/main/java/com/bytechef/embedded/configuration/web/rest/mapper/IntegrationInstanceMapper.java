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

import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.dto.IntegrationInstanceDTO;
import com.bytechef.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceBasicModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceModel;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class IntegrationInstanceMapper {

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceToIntegrationInstanceBasicModelMapper
        extends Converter<IntegrationInstance, IntegrationInstanceBasicModel> {

        @Override
        @Mapping(target = "environment", ignore = true)
        @Mapping(target = "lastExecutionDate", ignore = true)
        IntegrationInstanceBasicModel convert(IntegrationInstance integrationInstanc);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceToIntegrationInstanceModelMapper
        extends Converter<IntegrationInstance, IntegrationInstanceModel> {

        @Override
        @Mapping(target = "environment", ignore = true)
        @Mapping(target = "lastExecutionDate", ignore = true)
        @Mapping(target = "integrationInstanceConfiguration", ignore = true)
        IntegrationInstanceModel convert(IntegrationInstance integrationInstance);

        @InheritInverseConfiguration
        @DelegatingConverter
        IntegrationInstance invertConvert(IntegrationInstanceModel integrationInstanceModel);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceDTOToIntegrationInstanceBasicModelMapper
        extends Converter<IntegrationInstanceDTO, IntegrationInstanceBasicModel> {

        @Override
        @Mapping(target = "environment", ignore = true)
        @Mapping(target = "lastExecutionDate", ignore = true)
        IntegrationInstanceBasicModel convert(IntegrationInstanceDTO integrationInstanc);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceDTOToIntegrationInstanceModelMapper
        extends Converter<IntegrationInstanceDTO, IntegrationInstanceModel> {

        @Override
        @Mapping(target = "environment", ignore = true)
        @Mapping(target = "lastExecutionDate", ignore = true)
        @Mapping(target = "integrationInstanceConfiguration", ignore = true)
        IntegrationInstanceModel convert(IntegrationInstanceDTO integrationInstance);
    }
}
