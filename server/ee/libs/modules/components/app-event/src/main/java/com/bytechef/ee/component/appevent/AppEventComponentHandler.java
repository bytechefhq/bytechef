/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.appevent;

import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.component.ComponentHandler;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.ee.component.appevent.trigger.AppEventTrigger;
import com.bytechef.ee.embedded.configuration.service.AppEventService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import com.bytechef.platform.component.definition.AppEventComponentDefinition;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component(AppEventComponentDefinition.APP_EVENT + "_v1_ComponentHandler")
@ConditionalOnEEVersion
public class AppEventComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public AppEventComponentHandler(AppEventService appEventService) {
        this.componentDefinition = new AppEventComponentDefinitionImpl(appEventService);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class AppEventComponentDefinitionImpl extends AbstractComponentDefinitionWrapper
        implements AppEventComponentDefinition {

        public AppEventComponentDefinitionImpl(AppEventService appEventService) {
            super(component(APP_EVENT)
                .title("App Event")
                .description("Use one event from your app tot trigger workflows across any integrations.")
                .icon("path:assets/app-event.svg")
                .categories(ComponentCategory.HELPERS)
                .triggers(new AppEventTrigger(appEventService).triggerDefinition));
        }
    }
}
