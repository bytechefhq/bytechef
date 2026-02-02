/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.web.graphql;

import com.bytechef.atlas.coordinator.annotation.ConditionalOnCoordinator;
import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade.CustomComponentDefinitionRecord;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Controller
@ConditionalOnCoordinator
@ConditionalOnEEVersion
class CustomComponentGraphQlController {

    private final CustomComponentFacade customComponentFacade;
    private final CustomComponentService customComponentService;

    @SuppressFBWarnings("EI")
    CustomComponentGraphQlController(
        CustomComponentFacade customComponentFacade, CustomComponentService customComponentService) {

        this.customComponentFacade = customComponentFacade;
        this.customComponentService = customComponentService;
    }

    @QueryMapping
    CustomComponent customComponent(@Argument Long id) {
        return customComponentService.getCustomComponent(id);
    }

    @QueryMapping
    CustomComponentDefinitionRecord customComponentDefinition(@Argument Long id) {
        return customComponentFacade.getCustomComponentDefinition(id);
    }

    @QueryMapping
    List<CustomComponent> customComponents() {
        return customComponentFacade.getCustomComponents();
    }

    @MutationMapping
    boolean deleteCustomComponent(@Argument Long id) {
        customComponentFacade.delete(id);

        return true;
    }

    @MutationMapping
    boolean enableCustomComponent(@Argument Long id, @Argument boolean enable) {
        customComponentService.enableCustomComponent(id, enable);

        return true;
    }
}
