/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationWorkflowConnectionModel;
import org.mapstruct.Mapper;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
public interface IntegrationInstanceConfigurationWorkflowConnectionModelMapper
    extends
    Converter<IntegrationInstanceConfigurationWorkflowConnectionModel, IntegrationInstanceConfigurationWorkflowConnection> {

    @Override
    IntegrationInstanceConfigurationWorkflowConnection convert(
        IntegrationInstanceConfigurationWorkflowConnectionModel integrationInstanceConfigurationWorkflowConnectionModel);
}
