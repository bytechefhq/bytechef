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

package com.bytechef.component.delay.action;

import static com.bytechef.component.delay.constant.DelayConstants.MILLIS;
import static org.mockito.ArgumentMatchers.eq;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction;
import com.bytechef.component.definition.ActionDefinition.Suspend;
import com.bytechef.component.definition.ActionDefinition.SuspendPerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@Disabled
public class DelaySleepActionTest {

    @Test
    public void testSuspendPerform() throws Exception {
        Parameters inputParameters = Mockito.mock(Parameters.class);
        Parameters connectionParameters = Mockito.mock(Parameters.class);
        ActionContext context = Mockito.mock(ActionContext.class);

        Mockito.when(inputParameters.containsKey(eq(MILLIS)))
            .thenReturn(true);
        Mockito.when(inputParameters.getLong(eq(MILLIS)))
            .thenReturn(500L);

        ModifiableActionDefinition actionDefinition = DelaySleepAction.of();

        SuspendPerformFunction suspendPerformFunction = actionDefinition.getSuspendPerform()
            .orElseThrow();

        Suspend suspend = suspendPerformFunction.apply(inputParameters, connectionParameters, context);

        Assertions.assertNotNull(suspend);
        Assertions.assertNotNull(suspend.expiresAt());
        Assertions.assertNotNull(suspend.continueParameters());
        Assertions.assertEquals(500L, suspend.continueParameters()
            .get("delayMillis"));
    }

    @Test
    public void testResumePerform() throws Exception {
        Parameters inputParameters = Mockito.mock(Parameters.class);
        Parameters connectionParameters = Mockito.mock(Parameters.class);
        Parameters continueParameters = Mockito.mock(Parameters.class);
        ActionContext context = Mockito.mock(ActionContext.class);

        long expiresAtMillis = Instant.now()
            .toEpochMilli();

        Mockito.when(continueParameters.getLong(eq("expiresAt")))
            .thenReturn(expiresAtMillis);
        Mockito.when(continueParameters.getLong(eq("delayMillis")))
            .thenReturn(500L);

        ModifiableActionDefinition actionDefinition = DelaySleepAction.of();

        ResumePerformFunction resumePerformFunction = actionDefinition.getResumePerform()
            .orElseThrow();

        Object result = resumePerformFunction.apply(inputParameters, connectionParameters, continueParameters, context);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof Map);

        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;

        Assertions.assertTrue(resultMap.containsKey("scheduledAt"));
        Assertions.assertTrue(resultMap.get("scheduledAt") instanceof Instant);
        Assertions.assertEquals(500L, resultMap.get("delayMillis"));
    }
}
