/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.apiplatform.configuration.web.rest.mapper;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.domain.ProjectInstance;
import com.bytechef.automation.configuration.web.rest.model.ProjectBasicModel;
import com.bytechef.automation.configuration.web.rest.model.ProjectInstanceBasicModel;
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

    @Mapping(target = "lastExecutionDate", ignore = true)
    ProjectInstanceBasicModel mapToProjectInstanceModel(ProjectInstance projectInstance);

    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "tagIds", ignore = true)
    ProjectInstance mapToProjectInstance(ProjectInstanceBasicModel projectInstanceBasicModel);

    @Mapping(target = "workspaceId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tagIds", ignore = true)
    @Mapping(target = "categoryId", ignore = true)
    Project mapToProject(ProjectBasicModel projectBasicModel);
}
