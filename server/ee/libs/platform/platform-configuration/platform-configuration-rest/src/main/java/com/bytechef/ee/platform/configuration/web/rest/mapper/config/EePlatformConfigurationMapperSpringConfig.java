/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.web.rest.mapper.config;

import com.bytechef.ee.platform.configuration.web.rest.adapter.EePlatformConfigurationConversionServiceAdapter;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    EePlatformConfigurationConversionServiceAdapter.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.ee.platform.configuration.web.rest.adapter",
    conversionServiceAdapterClassName = "EePlatformConfigurationConversionServiceAdapter")
public interface EePlatformConfigurationMapperSpringConfig {
}
