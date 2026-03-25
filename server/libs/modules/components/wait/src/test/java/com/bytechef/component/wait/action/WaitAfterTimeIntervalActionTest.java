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

import static com.bytechef.component.wait.constant.WaitConstants.AMOUNT;
import static com.bytechef.component.wait.constant.WaitConstants.UNIT;
import static org.mockito.ArgumentMatchers.eq;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction;
import com.bytechef.component.definition.ActionDefinition.ResumePerformFunction.ResumeResponse;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * @author Ivica Cardic
 */
@Disabled
public class WaitAfterTimeIntervalActionTest {

    @Test
    public void testPerformCallsSuspend() throws Exception {
        Parameters inputParameters = Mockito.mock(Parameters.class);
        Parameters connectionParameters = Mockito.mock(Parameters.class);
        ActionContext context = Mockito.mock(ActionContext.class);

        Mockito.when(inputParameters.getRequiredInteger(eq(AMOUNT)))
            .thenReturn(5);
        Mockito.when(inputParameters.getRequiredString(eq(UNIT)))
            .thenReturn("MINUTES");

        ModifiableActionDefinition actionDefinition = WaitAfterTimeIntervalAction.of();

        PerformFunction performFunction = (PerformFunction) actionDefinition.getPerform()
            .orElseThrow();

        performFunction.apply(inputParameters, connectionParameters, context);

        ArgumentCaptor<Suspend> suspendCaptor = ArgumentCaptor.forClass(Suspend.class);

        Mockito.verify(context)
            .suspend(suspendCaptor.capture());

        Suspend suspend = suspendCaptor.getValue();

        Assertions.assertNotNull(suspend);
        Assertions.assertNotNull(suspend.expiresAt());
        Assertions.assertNotNull(suspend.continueParameters());
        Assertions.assertEquals(5, suspend.continueParameters()
            .get("amount"));
        Assertions.assertEquals("MINUTES", suspend.continueParameters()
            .get("unit"));
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
        Mockito.when(continueParameters.getInteger(eq("amount")))
            .thenReturn(5);
        Mockito.when(continueParameters.getString(eq("unit")))
            .thenReturn("MINUTES");

        ModifiableActionDefinition actionDefinition = WaitAfterTimeIntervalAction.of();

        ResumePerformFunction resumePerformFunction = actionDefinition.getResumePerform()
            .orElseThrow();

        Parameters data = Mockito.mock(Parameters.class);

        ResumeResponse result = resumePerformFunction.apply(
            inputParameters, connectionParameters, continueParameters, data, context);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.containsKey(ResumeResponse.DATA));
        Assertions.assertTrue(result.containsKey(ResumeResponse.RESUMED));
        Assertions.assertEquals(true, result.get(ResumeResponse.RESUMED));

        @SuppressWarnings("unchecked")
        Map<String, Object> dataMap = (Map<String, Object>) result.get(ResumeResponse.DATA);

        Assertions.assertTrue(dataMap.containsKey("scheduledAt"));
        Assertions.assertEquals(5, dataMap.get("amount"));
        Assertions.assertEquals("MINUTES", dataMap.get("unit"));
    }
}
