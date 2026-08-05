/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.customcomponent.configuration.web.graphql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.ee.platform.customcomponent.configuration.domain.CustomComponent;
import com.bytechef.ee.platform.customcomponent.configuration.facade.CustomComponentFacade;
import com.bytechef.ee.platform.customcomponent.configuration.service.CustomComponentService;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class CustomComponentGraphQlControllerTest {

    private final CustomComponentFacade customComponentFacade = mock(CustomComponentFacade.class);
    private final CustomComponentService customComponentService = mock(CustomComponentService.class);
    private final CustomComponentGraphQlController controller = new CustomComponentGraphQlController(
        customComponentFacade, customComponentService);

    @Test
    void testUpdateCustomComponentSourceReturnsUpdatedComponent() {
        CustomComponent customComponent = new CustomComponent();

        customComponent.setId(7L);

        when(customComponentFacade.updateCustomComponentSource(1L, "source")).thenReturn(customComponent);

        assertEquals(customComponent, controller.updateCustomComponentSource(1L, "source"));
    }

    @Test
    void testPublishCustomComponentDelegatesToFacade() {
        CustomComponent customComponent = new CustomComponent();

        when(customComponentFacade.publishCustomComponent(1L)).thenReturn(customComponent);

        assertEquals(customComponent, controller.publishCustomComponent(1L));
    }
}
