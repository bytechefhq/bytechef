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

package com.bytechef.platform.component.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.platform.component.constant.MetadataConstants;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SuspendUtilsTest {

    @Test
    void testReturnsNullWhenNoSuspendRequested() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        when(actionContextAware.getSuspend()).thenReturn(null);

        ActionContext.Suspend result = SuspendUtils.finalizeSuspend(actionContextAware);

        assertNull(result);
    }

    @Test
    void testReturnsSuspendUnchangedWhenJobResumeIdIsAbsent() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        Map<String, Object> continueParameters = new HashMap<>();

        continueParameters.put("answer", "yes");

        ActionContext.Suspend suspend =
            new ActionContext.Suspend(continueParameters, Instant.parse("2026-01-01T00:00:00Z"));

        when(actionContextAware.getSuspend()).thenReturn(suspend);
        when(actionContextAware.getJobResumeId()).thenReturn(null);

        ActionContext.Suspend result = SuspendUtils.finalizeSuspend(actionContextAware);

        assertSame(suspend, result, "When no jobResumeId is present, the suspend should be returned as-is");
    }

    @Test
    void testEnrichesContinueParametersWithJobResumeId() {
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        Map<String, Object> continueParameters = new HashMap<>();

        continueParameters.put("answer", "yes");

        Instant expiresAt = Instant.parse("2026-01-01T00:00:00Z");

        ActionContext.Suspend suspend = new ActionContext.Suspend(continueParameters, expiresAt);

        when(actionContextAware.getSuspend()).thenReturn(suspend);
        when(actionContextAware.getJobResumeId()).thenReturn("resume-token-abc");

        ActionContext.Suspend result = SuspendUtils.finalizeSuspend(actionContextAware);

        assertEquals(expiresAt, result.expiresAt(), "expiresAt must be preserved");
        assertEquals("yes", result.continueParameters()
            .get("answer"), "existing continueParameters must be preserved");
        assertEquals(
            "resume-token-abc",
            result.continueParameters()
                .get(MetadataConstants.JOB_RESUME_ID),
            "jobResumeId must be injected under MetadataConstants.JOB_RESUME_ID");
    }

    @Test
    void testDoesNotMutateOriginalContinueParametersMap() {
        // Defensive: the caller's map must remain pristine so the agent loop's continueParameters reference
        // is not modified through this seam.
        ActionContextAware actionContextAware = mock(ActionContextAware.class);

        Map<String, Object> originalContinueParameters = new HashMap<>();

        originalContinueParameters.put("answer", "yes");

        ActionContext.Suspend suspend = new ActionContext.Suspend(originalContinueParameters, Instant.now());

        when(actionContextAware.getSuspend()).thenReturn(suspend);
        when(actionContextAware.getJobResumeId()).thenReturn("resume-token-abc");

        SuspendUtils.finalizeSuspend(actionContextAware);

        org.junit.jupiter.api.Assertions.assertFalse(
            originalContinueParameters.containsKey(MetadataConstants.JOB_RESUME_ID),
            "Original continueParameters map must not be mutated");
    }
}
