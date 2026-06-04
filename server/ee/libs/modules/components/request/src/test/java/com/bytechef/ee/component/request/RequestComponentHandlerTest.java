/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.component.request;

import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 * @version ee
 */
public class RequestComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/request_v1.json", new RequestComponentHandler().getDefinition());
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
