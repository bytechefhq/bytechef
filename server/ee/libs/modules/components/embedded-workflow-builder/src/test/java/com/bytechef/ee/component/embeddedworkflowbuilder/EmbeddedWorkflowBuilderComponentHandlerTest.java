/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.embeddedworkflowbuilder;

import static org.mockito.Mockito.mock;

import com.bytechef.ee.embedded.configuration.facade.ConnectedUserProjectFacade;
import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
public class EmbeddedWorkflowBuilderComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        ConnectedUserProjectFacade facade = mock(ConnectedUserProjectFacade.class);

        JsonFileAssert.assertEquals(
            "definition/embeddedWorkflowBuilder_v1.json",
            new EmbeddedWorkflowBuilderComponentHandler(facade).getDefinition());
    }
}
