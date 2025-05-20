/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationWorkflowModel;
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
public class IntegrationInstanceConfigurationWorkflowMapper {

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceWorkflowToIntegrationInstanceWorkflowModelMapper
        extends Converter<IntegrationInstanceConfigurationWorkflow, IntegrationInstanceConfigurationWorkflowModel> {

        @Override
        @Mapping(target = "lastExecutionDate", ignore = true)
        @Mapping(target = "workflowReferenceCode", ignore = true)
        IntegrationInstanceConfigurationWorkflowModel convert(
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow);

        @InheritInverseConfiguration
        @DelegatingConverter
        IntegrationInstanceConfigurationWorkflow invertConvert(
            IntegrationInstanceConfigurationWorkflowModel integrationInstanceConfigurationWorkflowModel);
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public interface IntegrationInstanceWorkflowDTOToIntegrationInstanceWorkflowModelMapper
        extends Converter<IntegrationInstanceConfigurationWorkflowDTO, IntegrationInstanceConfigurationWorkflowModel> {

        @Override
        IntegrationInstanceConfigurationWorkflowModel convert(
            IntegrationInstanceConfigurationWorkflowDTO integrationInstanceWorkflow);

        @InheritInverseConfiguration
        @DelegatingConverter
        @Mapping(target = "workflow", ignore = true)
        IntegrationInstanceConfigurationWorkflowDTO invertConvert(
            IntegrationInstanceConfigurationWorkflowModel integrationInstanceConfigurationWorkflowModel);
    }
}
