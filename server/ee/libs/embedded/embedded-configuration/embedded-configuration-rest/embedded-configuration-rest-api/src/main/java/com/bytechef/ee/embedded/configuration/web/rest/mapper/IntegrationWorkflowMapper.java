/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.WorkflowBasicModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.WorkflowModel;
import com.bytechef.platform.configuration.web.rest.mapper.util.WorkflowMapperUtils;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public abstract class IntegrationWorkflowMapper {

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public abstract static class IntegrationWorkflowDTOToWorkflowModelMapper
        implements Converter<IntegrationWorkflowDTO, WorkflowModel> {

        @Override
        @Mapping(target = "connectionsCount", ignore = true)
        @Mapping(target = "inputsCount", ignore = true)
        @Mapping(target = "workflowTaskComponentNames", ignore = true)
        @Mapping(target = "workflowTriggerComponentNames", ignore = true)
        public abstract WorkflowModel convert(IntegrationWorkflowDTO workflowDTO);

        @AfterMapping
        public void afterMapping(IntegrationWorkflowDTO workflowDTO, @MappingTarget WorkflowModel workflowModel) {
            WorkflowMapperUtils.afterMapping(
                workflowDTO.getInputs(), workflowDTO.getTasks(), workflowDTO.getTriggers(), workflowModel);
        }
    }

    @Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
    public abstract static class IntegrationWorkflowModelToWorkflowBasicModel
        implements Converter<IntegrationWorkflowDTO, WorkflowBasicModel> {

        @Override
        public abstract WorkflowBasicModel convert(IntegrationWorkflowDTO workflowDTO);
    }
}
