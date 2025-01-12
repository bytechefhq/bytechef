/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.apiplatform;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.API_PLATFORM;

import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.apiplatform.action.ApiPlatformResponseToApiRequestAction;
import com.bytechef.ee.component.apiplatform.trigger.ApiPlatformNewApiRequestTrigger;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.ApiPlatformComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component(API_PLATFORM + "_v1_ComponentHandler")
public class ApiPlatformComponentHandler implements ComponentHandler {

    private final ApiPlatformComponentDefinition componentDefinition;

    public ApiPlatformComponentHandler(WorkflowService workflowService) {
        this.componentDefinition = new ApiPlatformComponentDefinitionImpl(
            component(API_PLATFORM)
                .title("API Platform")
                .description("Actions and triggers for using with API platform.")
                .icon("path:assets/api-platform.svg")
                .categories(ComponentCategory.HELPERS)
                .triggers(new ApiPlatformNewApiRequestTrigger().triggerDefinition)
                .actions(new ApiPlatformResponseToApiRequestAction(workflowService).actionDefinition));
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class ApiPlatformComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements ApiPlatformComponentDefinition {

        public ApiPlatformComponentDefinitionImpl(ComponentDefinition componentDefinition) {
            super(componentDefinition);
        }
    }
}
