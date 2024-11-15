/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper;

import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionEndpointDTO;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper.config.ApiPlatformMapperSpringConfig;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiCollectionEndpointModel;
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
@Mapper(config = ApiPlatformMapperSpringConfig.class)
public interface ApiCollectionEndpointMapper extends Converter<ApiCollectionEndpointDTO, ApiCollectionEndpointModel> {

    @Override
    @Mapping(target = "lastExecutionDate", ignore = true)
    ApiCollectionEndpointModel convert(ApiCollectionEndpointDTO apiCollectionEndpoint);

    @InheritInverseConfiguration
    @DelegatingConverter
    ApiCollectionEndpointDTO invertConvert(ApiCollectionEndpointModel apiCollectionEndpointModel);
}
