/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.request;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.request.trigger.RequestAutoRespondWithHTTP200Trigger;
import com.bytechef.ee.component.request.trigger.RequestAwaitWorkflowAndRespondTrigger;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 * @version ee
 */
@Component("request_v1_ComponentHandler")
@ConditionalOnEEVersion
public class RequestComponentHandler implements ComponentHandler {

    private static final ComponentDefinition COMPONENT_DEFINITION = component("request")
        .title("Request")
        .description(
            "Send an HTTP request from your application to a designated integration and workflow, with the option to receive a synchronous response.")
        .icon("path:assets/request.svg")
        .categories(ComponentCategory.HELPERS)
        .triggers(
            RequestAutoRespondWithHTTP200Trigger.TRIGGER_DEFINITION,
            RequestAwaitWorkflowAndRespondTrigger.TRIGGER_DEFINITION);

    @Override
    public ComponentDefinition getDefinition() {
        return COMPONENT_DEFINITION;
    }
}
