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

package com.bytechef.component.wait.action;

import static com.bytechef.component.wait.constant.WaitConstants.CSRF_TOKEN;
import static org.mockito.ArgumentMatchers.eq;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.component.definition.ActionDefinition.SuspendPerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@Disabled
public class WaitOnWebHookCallActionTest {

    @Test
    public void testSuspendPerform() throws Exception {
        Parameters inputParameters = Mockito.mock(Parameters.class);
        Parameters connectionParameters = Mockito.mock(Parameters.class);
        ActionContext context = Mockito.mock(ActionContext.class);

        Mockito.when(inputParameters.getRequiredString(eq(CSRF_TOKEN)))
            .thenReturn("test-token-123");

        ModifiableActionDefinition actionDefinition = WaitOnWebHookCallAction.of();

        SuspendPerformFunction suspendPerformFunction = actionDefinition.getSuspendPerform()
            .orElseThrow();

        Suspend suspend = suspendPerformFunction.apply(inputParameters, connectionParameters, context);

        Assertions.assertNotNull(suspend);
        Assertions.assertNotNull(suspend.expiresAt());
        Assertions.assertNotNull(suspend.continueParameters());
        Assertions.assertEquals("test-token-123", suspend.continueParameters()
            .get("csrfToken"));
    }

    @Test
    public void testBeforeSuspendIsPresent() {
        ModifiableActionDefinition actionDefinition = WaitOnWebHookCallAction.of();

        Assertions.assertTrue(actionDefinition.getBeforeSuspend()
            .isPresent());
    }
}
