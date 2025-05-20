/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceDTO;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceBasicModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceModel;
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
        @Mapping(target = "integrationInstanceWorkflows", ignore = true)
        IntegrationInstanceModel convert(IntegrationInstance integrationInstance);

        @InheritInverseConfiguration
        @DelegatingConverter
        IntegrationInstance invertConvert(IntegrationInstanceModel integrationInstanceModel);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceDTOToIntegrationInstanceBasicModelMapper
        extends Converter<IntegrationInstanceDTO, IntegrationInstanceBasicModel> {

        @Override
        IntegrationInstanceBasicModel convert(IntegrationInstanceDTO integrationInstance);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceDTOToIntegrationInstanceModelMapper
        extends Converter<IntegrationInstanceDTO, IntegrationInstanceModel> {

        @Override
        IntegrationInstanceModel convert(IntegrationInstanceDTO integrationInstance);
    }
}
