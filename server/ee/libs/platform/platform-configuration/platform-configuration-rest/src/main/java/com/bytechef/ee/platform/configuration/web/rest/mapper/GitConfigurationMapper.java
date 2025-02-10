/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.configuration.web.rest.mapper;

import com.bytechef.ee.platform.configuration.dto.GitConfigurationDTO;
import com.bytechef.ee.platform.configuration.web.rest.mapper.config.EePlatformConfigurationMapperSpringConfig;
import com.bytechef.ee.platform.configuration.web.rest.model.GitConfigurationModel;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.extensions.spring.DelegatingConverter;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EePlatformConfigurationMapperSpringConfig.class)
public interface GitConfigurationMapper extends Converter<GitConfigurationDTO, GitConfigurationModel> {

    @Override
    GitConfigurationModel convert(GitConfigurationDTO source);

    @InheritInverseConfiguration
    @DelegatingConverter
    GitConfigurationDTO invertConvert(GitConfigurationModel gitConfigurationModel);
}
