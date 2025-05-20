/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.web.rest.mapper.config;

import com.bytechef.ee.embedded.unified.web.rest.adapter.EmbeddedUnifiedConversionServiceAdapter;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import com.bytechef.web.rest.mapper.JsonNullableMapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    EmbeddedUnifiedConversionServiceAdapter.class, JsonNullableMapper.class, DateTimeMapper.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.ee.embedded.unified.web.rest.adapter",
    conversionServiceAdapterClassName = "EmbeddedUnifiedConversionServiceAdapter")
public interface EmbeddedUnifiedMapperSpringConfig {
}
