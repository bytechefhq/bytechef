/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper;

import com.bytechef.ee.automation.apiplatform.configuration.domain.ApiClient;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper.config.ApiPlatformConfigurationMapperSpringConfig;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiClientModel;
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
@Mapper(config = ApiPlatformConfigurationMapperSpringConfig.class)
public interface ApiClientMapper extends Converter<ApiClient, ApiClientModel> {

    @Override
    ApiClientModel convert(ApiClient apiClient);

    @InheritInverseConfiguration
    @DelegatingConverter
    @Mapping(target = "environment", ignore = true)
    ApiClient invertConvert(ApiClientModel apiClientModel);
}
