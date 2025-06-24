/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.dto.ConnectedUserProjectWorkflowDTO;
import com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.config.EmbeddedConfigurationPublicMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ConnectedUserProjectWorkflowModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(
    config = EmbeddedConfigurationPublicMapperSpringConfig.class, implementationName = "EmbeddedPublic<CLASS_NAME>Impl")
public interface ConnectUserProjectWorkflowMapper
    extends Converter<ConnectedUserProjectWorkflowDTO, ConnectedUserProjectWorkflowModel> {

    @Override
    @Mapping(target = "definition", source = "workflow.definition")
    @Mapping(target = "description", source = "workflow.description")
    @Mapping(target = "label", source = "workflow.label")
    ConnectedUserProjectWorkflowModel convert(ConnectedUserProjectWorkflowDTO connectedUserProjectWorkflowDTO);
}
