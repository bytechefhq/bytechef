/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper.config;

import com.bytechef.ee.automation.apiplatform.configuration.web.rest.adapter.ApiPlatformConfigurationConversionServiceAdapter;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    DateTimeMapper.class, ApiPlatformConfigurationConversionServiceAdapter.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.ee.automation.apiplatform.configuration.web.rest.adapter",
    conversionServiceAdapterClassName = "ApiPlatformConfigurationConversionServiceAdapter")
public interface ApiPlatformConfigurationMapperSpringConfig {
}
