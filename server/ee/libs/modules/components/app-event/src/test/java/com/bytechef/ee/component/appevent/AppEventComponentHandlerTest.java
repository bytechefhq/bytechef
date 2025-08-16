/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.appevent;

import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AppEventComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/app-event_v1.json", new AppEventComponentHandler(null).getDefinition());
    }

    @Test
    @Disabled
    public void testAutoRespondWithHTTP200Trigger() {
        // TODO
    }

    @Test
    @Disabled
    public void testAwaitWorkflowAndRespondTrigger() {
        // TODO
    }

    @Test
    @Disabled
    public void testValidateAndRespondTrigger() {
        // TODO
    }
}
