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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    AutomationWorkflowProjectModel convert(AutomationWorkflowProjectDTO automationWorkflowProjectDTO);

    AutomationWorkflowProjectComponentModel toComponentModel(ConnectedUserWorkflowTemplateDTO.Component component);

    @Mapping(target = "components", expression = "java(toComponentModels(workflowTemplateDTO))")
    @Mapping(target = "id", source = "workflowUuid")
    AutomationWorkflowProjectWorkflowTemplateModel toWorkflowTemplateModel(
        ConnectedUserWorkflowTemplateDTO workflowTemplateDTO);

    default List<AutomationWorkflowProjectComponentModel> toComponentModels(
        ConnectedUserWorkflowTemplateDTO workflowTemplateDTO) {

        Map<String, ConnectedUserWorkflowTemplateDTO.Component> componentsByName = Stream
            .of(workflowTemplateDTO.triggers(), workflowTemplateDTO.components())
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .collect(
                Collectors.toMap(
                    ConnectedUserWorkflowTemplateDTO.Component::name, component -> component,
                    (firstComponent, secondComponent) -> firstComponent, LinkedHashMap::new));

        return componentsByName.values()
            .stream()
            .map(this::toComponentModel)
            .toList();
    }
}
