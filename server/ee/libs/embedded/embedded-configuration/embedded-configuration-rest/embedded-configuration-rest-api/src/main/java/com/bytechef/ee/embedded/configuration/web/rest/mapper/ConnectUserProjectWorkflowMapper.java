/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.web.rest.mapper.config.EmbeddedConfigurationMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConnectedUserProjectWorkflowModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.WorkflowModel;
import com.bytechef.platform.configuration.dto.WorkflowDTO;
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
@Mapper(config = EmbeddedConfigurationMapperSpringConfig.class)
public interface ConnectUserProjectWorkflowMapper
    extends Converter<ConnectedUserProjectWorkflowDTO, ConnectedUserProjectWorkflowModel> {

    @Override
    ConnectedUserProjectWorkflowModel convert(ConnectedUserProjectWorkflowDTO connectedUserProjectWorkflowDTO);

    @Mapping(target = "connectionsCount", ignore = true)
    @Mapping(target = "inputsCount", ignore = true)
    @Mapping(target = "integrationWorkflowId", ignore = true)
    @Mapping(target = "workflowReferenceCode", ignore = true)
    @Mapping(target = "workflowTaskComponentNames", ignore = true)
    @Mapping(target = "workflowTriggerComponentNames", ignore = true)
    WorkflowModel map(WorkflowDTO workflowDTO);

    @AfterMapping
    default void afterMapping(WorkflowDTO workflowDTO, @MappingTarget WorkflowModel workflowModel) {
        WorkflowMapperUtils.afterMapping(
            workflowDTO.getInputs(), workflowDTO.getTasks(), workflowDTO.getTriggers(), workflowModel);
    }
}
