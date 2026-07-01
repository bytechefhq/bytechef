/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.web.rest.mapper;

import com.bytechef.ee.embedded.configuration.web.rest.model.ComponentDefinitionBasicModel;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.PropertyGroup;
import com.bytechef.platform.configuration.web.rest.mapper.config.PlatformConfigurationMapperSpringConfig;
import java.util.List;
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
public class ComponentDefinitionMapper {

    @Mapper(
        config = PlatformConfigurationMapperSpringConfig.class, implementationName = "Embedded<CLASS_NAME>Impl")
    public interface ComponentDefinitionToComponentDefinitionBasicModelMapper
        extends Converter<ComponentDefinition, ComponentDefinitionBasicModel> {

        @Mapping(target = "inputsCount", ignore = true)
        ComponentDefinitionBasicModel convert(ComponentDefinition componentDefinition);

        @AfterMapping
        default void afterMapping(
            ComponentDefinition componentDefinition,
            @MappingTarget ComponentDefinitionBasicModel componentDefinitionBasicModel) {

            componentDefinitionBasicModel.setIcon("/icons/%s.svg".formatted(componentDefinition.getName()));

            List<PropertyGroup> inputs = componentDefinition.getInputs();

            componentDefinitionBasicModel.setInputsCount(inputs.size());
        }
    }
}
