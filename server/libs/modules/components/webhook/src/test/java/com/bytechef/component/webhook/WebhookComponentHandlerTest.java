/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.component.webhook;

import com.bytechef.test.jsonasssert.JsonFileAssert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class WebhookComponentHandlerTest {

    @Test
    public void testGetComponentDefinition() {
        JsonFileAssert.assertEquals("definition/webhook_v1.json", new WebhookComponentHandler().getDefinition());
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
