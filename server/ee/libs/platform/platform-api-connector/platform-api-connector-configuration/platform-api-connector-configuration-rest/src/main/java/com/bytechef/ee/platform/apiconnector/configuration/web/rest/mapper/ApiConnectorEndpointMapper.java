/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.web.rest.mapper;

import com.bytechef.ee.platform.apiconnector.configuration.domain.ApiConnectorEndpoint;
import com.bytechef.ee.platform.apiconnector.configuration.web.rest.mapper.config.EePlatformApiConnectorConfigurationMapperSpringConfig;
import com.bytechef.ee.platform.apiconnector.configuration.web.rest.model.ApiConnectorEndpointModel;
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

@Mapper(config = EePlatformApiConnectorConfigurationMapperSpringConfig.class)
public interface ApiConnectorEndpointMapper extends Converter<ApiConnectorEndpoint, ApiConnectorEndpointModel> {

    @Override
    @Mapping(target = "lastExecutionDate", ignore = true)
    ApiConnectorEndpointModel convert(ApiConnectorEndpoint apiConnectorEndpoint);

    @InheritInverseConfiguration
    @DelegatingConverter
    @Mapping(target = "apiConnectorId", ignore = true)
    ApiConnectorEndpoint invertConvert(ApiConnectorEndpointModel apiConnectorEndpointModel);
}
