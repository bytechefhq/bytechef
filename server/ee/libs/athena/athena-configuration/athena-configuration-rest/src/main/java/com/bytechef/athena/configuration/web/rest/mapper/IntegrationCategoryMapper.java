/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.athena.configuration.web.rest.mapper;

import com.bytechef.athena.configuration.web.rest.mapper.config.IntegrationMapperSpringConfiguration;
import com.bytechef.athena.configuration.web.rest.model.CategoryModel;
import com.bytechef.category.domain.Category;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = IntegrationMapperSpringConfiguration.class)
public interface IntegrationCategoryMapper extends Converter<Category, CategoryModel> {

    CategoryModel convert(Category category);
}
