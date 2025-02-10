/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.configuration.web.rest.mapper;

import com.bytechef.ee.automation.configuration.domain.ProjectGitConfiguration;
import com.bytechef.ee.automation.configuration.web.rest.mapper.config.EeAutomationConfigurationMapperSpringConfig;
import com.bytechef.ee.automation.configuration.web.rest.model.ProjectGitConfigurationModel;
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
@Mapper(config = EeAutomationConfigurationMapperSpringConfig.class)
public interface ProjectGitConfigurationMapper
    extends Converter<ProjectGitConfiguration, ProjectGitConfigurationModel> {

    @Override
    ProjectGitConfigurationModel convert(ProjectGitConfiguration projectGitConfiguration);

    @InheritInverseConfiguration
    @DelegatingConverter
    @Mapping(target = "id", ignore = true)
    ProjectGitConfiguration invertConvert(ProjectGitConfigurationModel projectGitConfigurationModel);
}
