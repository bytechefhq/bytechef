/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.web.rest.mapper;

import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.web.rest.mapper.config.EePlatformCustomComponentConfigurationMapperSpringConfig;
import com.bytechef.ee.platform.customcomponent.configuration.web.rest.model.CustomComponentModel;
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
@Mapper(config = EePlatformCustomComponentConfigurationMapperSpringConfig.class)
public interface CustomComponentMapper extends Converter<CustomComponent, CustomComponentModel> {

    @Override
    CustomComponentModel convert(CustomComponent customComponent);

    @InheritInverseConfiguration
    @DelegatingConverter
    @Mapping(target = "component", ignore = true)
    CustomComponent invertConvert(CustomComponentModel customComponentModel);
}
