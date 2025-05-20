/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationBasicModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationModel;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationInstanceConfigurationMapper {

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceConfigurationBasicToIntegrationInstanceModelMapper
        extends Converter<IntegrationInstanceConfiguration, IntegrationInstanceConfigurationBasicModel> {

        @Override
        IntegrationInstanceConfigurationBasicModel convert(IntegrationInstanceConfiguration integrationInstance);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceConfigurationToIntegrationInstanceModelMapper
        extends Converter<IntegrationInstanceConfiguration, IntegrationInstanceConfigurationModel> {

        @Mapping(target = "connectionAuthorizationParameters", ignore = true)
        @Mapping(target = "connectionConnectionParameters", ignore = true)
        @Mapping(target = "integration", ignore = true)
        @Mapping(target = "integrationInstanceConfigurationWorkflows", ignore = true)
        @Mapping(target = "tags", ignore = true)
        @Override
        IntegrationInstanceConfigurationModel convert(IntegrationInstanceConfiguration integrationInstance);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceConfigurationDTOToIntegrationInstanceModelMapper
        extends Converter<IntegrationInstanceConfigurationDTO, IntegrationInstanceConfigurationModel> {

        @Override
        IntegrationInstanceConfigurationModel convert(
            IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO);

        @InheritInverseConfiguration
        @DelegatingConverter
        @Mapping(target = "integration", ignore = true)
        IntegrationInstanceConfigurationDTO invertConvert(
            IntegrationInstanceConfigurationModel integrationInstanceConfigurationModel);
    }
}
