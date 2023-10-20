/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.web.rest.mapper.config;

import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring")
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.athena.configuration.web.rest.adapter",
    conversionServiceAdapterClassName = "IntegrationConversionServiceAdapter")
public interface IntegrationMapperSpringConfiguration {
}
