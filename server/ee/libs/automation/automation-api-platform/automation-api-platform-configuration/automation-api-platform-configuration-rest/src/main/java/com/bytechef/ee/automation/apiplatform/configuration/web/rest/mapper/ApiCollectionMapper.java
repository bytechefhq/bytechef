/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper;

import com.bytechef.ee.automation.apiplatform.configuration.dto.ApiCollectionDTO;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper.config.AutomationApiPlatformMapperSpringConfig;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.ApiCollectionModel;
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
@Mapper(config = AutomationApiPlatformMapperSpringConfig.class)
public interface ApiCollectionMapper extends Converter<ApiCollectionDTO, ApiCollectionModel> {

    @Override
    @Mapping(target = "workspaceId", ignore = true)
    ApiCollectionModel convert(ApiCollectionDTO apiCollectionDTO);

    @InheritInverseConfiguration
    @DelegatingConverter
    ApiCollectionDTO invertConvert(ApiCollectionModel apiCollectionModel);
}
