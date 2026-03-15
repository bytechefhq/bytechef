/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.config;

import com.bytechef.ee.embedded.configuration.public_.web.rest.adapter.EmbeddedConfigurationPublicConversionServiceAdapter;
import com.bytechef.platform.configuration.web.rest.mapper.EnvironmentMapper;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    DateTimeMapper.class, EnvironmentMapper.class, EmbeddedConfigurationPublicConversionServiceAdapter.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.ee.embedded.configuration.public_.web.rest.adapter",
    conversionServiceAdapterClassName = "EmbeddedConfigurationPublicConversionServiceAdapter")
public interface EmbeddedConfigurationPublicMapperSpringConfig {
}
