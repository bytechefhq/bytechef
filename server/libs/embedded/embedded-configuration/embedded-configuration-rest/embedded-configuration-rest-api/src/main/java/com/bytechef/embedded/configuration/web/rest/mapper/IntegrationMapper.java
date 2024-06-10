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

import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.embedded.configuration.web.rest.mapper.config.IntegratioConfigurationMapperSpringConfig;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationBasicModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @author Ivica Cardic
 */
public class IntegrationMapper {

    @Mapper(config = IntegratioConfigurationMapperSpringConfig.class)
    public interface IntegrationToIntegrationBasicModelMapper extends Converter<Integration, IntegrationBasicModel> {

        @Override
        @Mapping(target = "integrationVersion", source = "lastVersion")
        @Mapping(target = "publishedDate", source = "lastPublishedDate")
        @Mapping(target = "status", source = "lastStatus")
        IntegrationBasicModel convert(Integration integration);
    }

    @Mapper(config = IntegratioConfigurationMapperSpringConfig.class)
    public interface IntegrationDTOToIntegrationModelMapper extends Converter<IntegrationDTO, IntegrationModel> {

        @Override
        IntegrationModel convert(IntegrationDTO integrationDTO);
    }
}
