/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.public_.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.dto.AutomationWorkflowProjectDTO;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserWorkflowTemplateDTO;
import com.bytechef.ee.embedded.configuration.public_.web.rest.mapper.config.EmbeddedConfigurationPublicMapperSpringConfig;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationWorkflowProjectComponentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationWorkflowProjectModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationWorkflowProjectWorkflowTemplateModel;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.core.convert.converter.Converter;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Mapper(
    config = EmbeddedConfigurationPublicMapperSpringConfig.class,
    implementationName = "EmbeddedPublic<CLASS_NAME>Impl")
public interface AutomationWorkflowProjectMapper
    extends Converter<AutomationWorkflowProjectDTO, AutomationWorkflowProjectModel> {

    @Override
    @Mapping(target = "kind", source = "codeWorkflowProject")
    AutomationWorkflowProjectModel convert(AutomationWorkflowProjectDTO automationWorkflowProjectDTO);

    AutomationWorkflowProjectComponentModel toComponentModel(ConnectedUserWorkflowTemplateDTO.Component component);

    @Mapping(target = "id", source = "workflowUuid")
    AutomationWorkflowProjectWorkflowTemplateModel toWorkflowTemplateModel(
        ConnectedUserWorkflowTemplateDTO workflowTemplateDTO);

    /**
     * A catalog project's {@code kind} tells clients whether copying one of its workflow templates creates a per-user
     * copy ({@code COPY}) or a shared reference ({@code REFERENCE}); the boolean-to-enum conversion is expressed here
     * so MapStruct can wire it directly into the generated {@link #convert} implementation.
     */
    default AutomationWorkflowProjectModel.KindEnum mapKind(boolean codeWorkflowProject) {
        return codeWorkflowProject
            ? AutomationWorkflowProjectModel.KindEnum.REFERENCE
            : AutomationWorkflowProjectModel.KindEnum.COPY;
    }
}
