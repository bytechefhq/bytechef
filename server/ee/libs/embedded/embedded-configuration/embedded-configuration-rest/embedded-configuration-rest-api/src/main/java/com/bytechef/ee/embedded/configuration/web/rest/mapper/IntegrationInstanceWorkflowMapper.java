/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceWorkflowModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class IntegrationInstanceWorkflowMapper {

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceWorkflowDToIntegrationInstanceWorkflowModelMapper
        extends Converter<IntegrationInstanceWorkflow, IntegrationInstanceWorkflowModel> {

        @Override
        @Mapping(target = "workflowId", ignore = true)
        IntegrationInstanceWorkflowModel convert(IntegrationInstanceWorkflow integrationInstanceWorkflow);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceWorkflowDTOToIntegrationInstanceWorkflowModelMapper
        extends Converter<IntegrationInstanceWorkflowDTO, IntegrationInstanceWorkflowModel> {

        @Override
        IntegrationInstanceWorkflowModel convert(IntegrationInstanceWorkflowDTO integrationInstanceWorkflow);
    }
}
