/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.apiconnector.configuration.web.rest.mapper.config;

import com.bytechef.ee.platform.apiconnector.configuration.web.rest.adapter.EePlatformApiConnectorConfigurationConversionServiceAdapter;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    DateTimeMapper.class, EePlatformApiConnectorConfigurationConversionServiceAdapter.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.ee.platform.apiconnector.configuration.web.rest.adapter",
    conversionServiceAdapterClassName = "EePlatformApiConnectorConfigurationConversionServiceAdapter")
public interface EePlatformApiConnectorConfigurationMapperSpringConfig {
}
