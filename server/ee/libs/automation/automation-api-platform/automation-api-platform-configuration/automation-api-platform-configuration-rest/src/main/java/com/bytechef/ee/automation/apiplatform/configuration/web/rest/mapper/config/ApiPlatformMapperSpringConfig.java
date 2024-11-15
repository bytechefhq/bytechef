/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper.config;

import com.bytechef.ee.automation.apiplatform.configuration.web.rest.adapter.ApiPlatformConversionServiceAdapter;
import com.bytechef.platform.tag.web.rest.mapper.config.TagMapperSpringConfig;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    ApiPlatformConversionServiceAdapter.class, TagMapperSpringConfig.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.ee.automation.apiplatform.configuration.web.rest.adapter",
    conversionServiceAdapterClassName = "ApiPlatformConversionServiceAdapter")
public interface ApiPlatformMapperSpringConfig {
}
