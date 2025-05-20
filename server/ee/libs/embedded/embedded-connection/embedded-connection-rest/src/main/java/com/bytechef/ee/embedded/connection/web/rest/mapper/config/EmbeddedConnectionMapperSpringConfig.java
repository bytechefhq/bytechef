/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connection.web.rest.mapper.config;

import com.bytechef.ee.embedded.connection.web.rest.adapter.EmbeddedConnectionConversionServiceAdapter;
import com.bytechef.web.rest.mapper.DateTimeMapper;
import org.mapstruct.MapperConfig;
import org.mapstruct.extensions.spring.SpringMapperConfig;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@MapperConfig(componentModel = "spring", uses = {
    DateTimeMapper.class, EmbeddedConnectionConversionServiceAdapter.class
})
@SpringMapperConfig(
    conversionServiceAdapterPackage = "com.bytechef.ee.embedded.connection.web.rest.adapter",
    conversionServiceAdapterClassName = "EmbeddedConnectionConversionServiceAdapter")
public interface EmbeddedConnectionMapperSpringConfig {
}
