/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationBasicModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.ProvisionableIntegrationModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProvisionableIntegrationMapper {

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface ProvisionableIntegrationDTOToProvisionableIntegrationModelMapper
        extends Converter<ProvisionableIntegrationDTO, ProvisionableIntegrationModel> {

        @Override
        ProvisionableIntegrationModel convert(ProvisionableIntegrationDTO provisionableIntegrationDTO);

        IntegrationBasicModel toIntegrationBasicModel(IntegrationDTO integrationDTO);
    }
}
