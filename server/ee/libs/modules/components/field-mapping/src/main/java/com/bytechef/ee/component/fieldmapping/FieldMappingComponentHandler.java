/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.fieldmapping;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.ee.component.fieldmapping.constant.FieldMappingConstants.FIELD_MAPPING;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.fieldmapping.action.FieldMappingMapAction;
import com.bytechef.ee.component.fieldmapping.mapper.FieldMappingDirection;
import com.bytechef.ee.component.fieldmapping.resolver.FieldMappingDescriptorResolver;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.service.WorkflowTestConfigurationService;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component(FIELD_MAPPING + "_v1_ComponentHandler")
@ConditionalOnEEVersion
public class FieldMappingComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public FieldMappingComponentHandler(
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, WorkflowService workflowService,
        WorkflowTestConfigurationService workflowTestConfigurationService) {

        FieldMappingDescriptorResolver resolver = new FieldMappingDescriptorResolver(
            integrationInstanceWorkflowService, workflowService, workflowTestConfigurationService);

        this.componentDefinition = component(FIELD_MAPPING)
            .title("Field Mapping")
            .description(
                "Applies the connected user's field mapping to a payload, renaming keys between your application's " +
                    "fields and the integration's fields in either direction.")
            .icon("path:assets/field-mapping.svg")
            .categories(ComponentCategory.HELPERS)
            .actions(
                FieldMappingMapAction.of(FieldMappingDirection.TO_INTEGRATION, resolver),
                FieldMappingMapAction.of(FieldMappingDirection.TO_APPLICATION, resolver));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }
}
