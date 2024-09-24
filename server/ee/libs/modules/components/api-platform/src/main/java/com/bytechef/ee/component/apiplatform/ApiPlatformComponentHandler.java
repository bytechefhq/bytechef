/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.apiplatform;

import static com.bytechef.component.definition.ComponentDsl.component;
import static com.bytechef.ee.component.apiplatform.constant.ApiPlatformConstants.API_PLATFORM;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.apiplatform.action.ApiPlatformResponseToApiRequestAction;
import com.bytechef.ee.component.apiplatform.trigger.ApiPlatformNewAPIRequestTrigger;
import com.google.auto.service.AutoService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@AutoService(ComponentHandler.class)
public class ApiPlatformComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component(API_PLATFORM)
        .title("API Platform")
        .description("Actions and triggers for using with API platform.")
        .icon("path:assets/api-platform.svg")
        .categories(ComponentCategory.HELPERS)
        .triggers(ApiPlatformNewAPIRequestTrigger.TRIGGER_DEFINITION)
        .actions(ApiPlatformResponseToApiRequestAction.ACTION_DEFINITION);

    @Override
    @SuppressFBWarnings("EI")
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
