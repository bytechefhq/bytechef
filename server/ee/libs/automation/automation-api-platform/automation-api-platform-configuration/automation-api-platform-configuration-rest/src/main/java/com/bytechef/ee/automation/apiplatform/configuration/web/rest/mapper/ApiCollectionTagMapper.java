/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper;

import com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper.config.ApiPlatformConfigurationMapperSpringConfig;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.TagModel;
import com.bytechef.platform.tag.domain.Tag;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = ApiPlatformConfigurationMapperSpringConfig.class)
public interface ApiCollectionTagMapper extends Converter<Tag, TagModel> {

    @Override
    TagModel convert(Tag source);
}
