/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.web.rest.mapper;

import com.bytechef.athena.configuration.web.rest.mapper.config.IntegrationMapperSpringConfiguration;
import com.bytechef.athena.configuration.web.rest.model.TagModel;
import com.bytechef.tag.domain.Tag;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = IntegrationMapperSpringConfiguration.class)
public interface IntegrationTagModelMapper extends Converter<TagModel, Tag> {

    Tag convert(TagModel tagModel);
}
