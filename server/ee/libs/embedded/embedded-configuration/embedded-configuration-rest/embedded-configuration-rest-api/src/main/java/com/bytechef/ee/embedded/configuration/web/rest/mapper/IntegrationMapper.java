/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationBasicModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationModel;
import org.mapstruct.AfterMapping;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationMapper {

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationToIntegrationBasicModelMapper extends Converter<Integration, IntegrationBasicModel> {

        @AfterMapping
        default void afterMapping(Integration integration, @MappingTarget IntegrationBasicModel integrationBasicModel) {
            integrationBasicModel.setIcon("/icons/%s.svg".formatted(integration.getComponentName()));
        }

        @Override
        @Mapping(target = "icon", ignore = true)
        IntegrationBasicModel convert(Integration integration);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationDTOToIntegrationModelMapper extends Converter<IntegrationDTO, IntegrationModel> {

        @AfterMapping
        default void afterMapping(Integration integration, @MappingTarget IntegrationModel integrationModel) {
            integrationModel.setIcon("/icons/%s.svg".formatted(integration.getComponentName()));
        }

        @Override
        IntegrationModel convert(IntegrationDTO integrationDTO);

        @InheritInverseConfiguration
        @DelegatingConverter
        @Mapping(target = "componentVersion", ignore = true)
        @Mapping(target = "integrationVersions", ignore = true)
        IntegrationDTO invertConvert(IntegrationModel integrationModel);
    }
}
