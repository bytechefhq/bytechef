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

package com.bytechef.platform.component.definition.ai.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.Context;
import com.bytechef.platform.component.definition.ActionContextAware;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ClusterElementCallTargetTest {

    @Test
    void testOfReturnsEmptyForAContextWithoutJobIds() {
        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getJobId()).thenReturn(null);
        when(context.getTaskExecutionId()).thenReturn(5L);

        assertTrue(ClusterElementCallTarget.of(context)
            .isEmpty());
    }

    @Test
    void testOfReturnsEmptyForAPlainContext() {
        assertTrue(ClusterElementCallTarget.of(mock(Context.class))
            .isEmpty());
    }

    @Test
    void testOfReturnsTargetWithBothIds() {
        ActionContextAware context = mock(ActionContextAware.class);

        when(context.getJobId()).thenReturn(4L);
        when(context.getTaskExecutionId()).thenReturn(5L);

        Optional<ClusterElementCallTarget> target = ClusterElementCallTarget.of(context);

        assertTrue(target.isPresent());
        assertEquals(4L, target.get()
            .jobId());
        assertEquals(5L, target.get()
            .taskExecutionId());
        assertSame(ClusterElementCallRecorder.getInstance(), target.get()
            .recorder());
    }
}
