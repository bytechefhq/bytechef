/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.web.rest.mapper.config;

import com.bytechef.ee.platform.customcomponent.configuration.web.rest.adapter.CustomComponentConversionServiceAdapter;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    DateTimeMapper.class, CustomComponentConversionServiceAdapter.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.ee.platform.customcomponent.configuration.web.rest.adapter",
    conversionServiceAdapterClassName = "CustomComponentConversionServiceAdapter")
public interface CustomComponentMapperSpringConfig {
}
