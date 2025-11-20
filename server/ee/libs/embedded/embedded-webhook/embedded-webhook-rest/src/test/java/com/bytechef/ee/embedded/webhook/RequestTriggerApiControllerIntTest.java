/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.webhook;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Disabled
@WebMvcTest(value = RequestTriggerApiController.class)
public class RequestTriggerApiControllerIntTest {

    @Test
    public void testWebhooks() {
        // TODO
    }
}
