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

package com.bytechef.platform.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class ConcurrentExecutionGateTest {

    private final ConcurrentExecutionGate concurrentExecutionGate = new InMemoryConcurrentExecutionGate();

    @Test
    public void testAcquireUpToLimitThenReject() {
        assertThat(concurrentExecutionGate.tryAcquire("executions:tenant-a", 2)).isTrue();
        assertThat(concurrentExecutionGate.tryAcquire("executions:tenant-a", 2)).isTrue();
        assertThat(concurrentExecutionGate.tryAcquire("executions:tenant-a", 2)).isFalse();

        assertThat(concurrentExecutionGate.held("executions:tenant-a")).isEqualTo(2);
    }

    @Test
    public void testReleaseFreesSlot() {
        assertThat(concurrentExecutionGate.tryAcquire("executions:tenant-a", 1)).isTrue();
        assertThat(concurrentExecutionGate.tryAcquire("executions:tenant-a", 1)).isFalse();

        concurrentExecutionGate.release("executions:tenant-a");

        assertThat(concurrentExecutionGate.tryAcquire("executions:tenant-a", 1)).isTrue();
    }

    @Test
    public void testReleaseFloorsAtZero() {
        concurrentExecutionGate.release("executions:tenant-a");
        concurrentExecutionGate.release("executions:tenant-a");

        assertThat(concurrentExecutionGate.held("executions:tenant-a")).isEqualTo(0);
        assertThat(concurrentExecutionGate.tryAcquire("executions:tenant-a", 1)).isTrue();
        assertThat(concurrentExecutionGate.tryAcquire("executions:tenant-a", 1)).isFalse();
    }

    @Test
    public void testKeysAreIndependent() {
        assertThat(concurrentExecutionGate.tryAcquire("executions:tenant-a", 1)).isTrue();
        assertThat(concurrentExecutionGate.tryAcquire("executions:tenant-b", 1)).isTrue();
    }
}
