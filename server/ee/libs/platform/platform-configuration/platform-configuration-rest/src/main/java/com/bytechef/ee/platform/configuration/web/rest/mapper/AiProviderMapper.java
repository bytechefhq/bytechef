/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.web.rest.mapper;

import com.bytechef.ee.platform.configuration.dto.AiProviderDTO;
import com.bytechef.ee.platform.configuration.web.rest.mapper.config.EePlatformConfigurationMapperSpringConfig;
import com.bytechef.ee.platform.configuration.web.rest.model.AiProviderModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EePlatformConfigurationMapperSpringConfig.class)
public interface AiProviderMapper extends Converter<AiProviderDTO, AiProviderModel> {

    @Override
    AiProviderModel convert(AiProviderDTO source);
}
