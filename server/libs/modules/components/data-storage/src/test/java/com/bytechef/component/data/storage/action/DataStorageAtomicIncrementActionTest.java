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

package com.bytechef.component.data.storage.action;

import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.VALUE_TO_ADD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionContext.Data;
import com.bytechef.component.definition.ActionContext.Data.Scope;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataStorageAtomicIncrementActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Data mockedData = mock(Data.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(KEY, "counter", SCOPE, "WORKFLOW", VALUE_TO_ADD, 5));

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        when(mockedActionContext.data(any())).thenAnswer(
            invocation -> {
                ContextFunction<Data, ?> dataFunction = invocation.getArgument(0);

                return dataFunction.apply(mockedData);
            });
    }

    @Test
    void testPerformStartsFromZeroWhenKeyIsMissing() throws InterruptedException {
        when(mockedData.fetch(Scope.WORKFLOW, "counter")).thenReturn(Optional.empty());

        Integer result = DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(5, result);

        verify(mockedData).put(Scope.WORKFLOW, "counter", 5);
    }

    @Test
    void testPerformAddsToExistingValue() throws InterruptedException {
        when(mockedData.fetch(Scope.WORKFLOW, "counter")).thenReturn(Optional.of(52L));

        Integer result = DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext);

        assertEquals(57, result);

        verify(mockedData).put(Scope.WORKFLOW, "counter", 57);
    }

    @Test
    void testPerformRejectsNonNumericValue() {
        when(mockedData.fetch(Scope.WORKFLOW, "counter")).thenReturn(Optional.of("text"));

        assertThrows(
            IllegalArgumentException.class,
            () -> DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext));

        verify(mockedData, never()).put(any(), any(), any());
    }

    @Test
    void testPerformRejectsStoredValueOutsideIntegerRange() {
        when(mockedData.fetch(Scope.WORKFLOW, "counter")).thenReturn(Optional.of((long) Integer.MAX_VALUE + 1));

        assertThatThrownBy(
            () -> DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'counter'");

        verify(mockedData, never()).put(any(), any(), any());
    }

    @Test
    void testPerformRejectsIncrementThatOverflows() {
        when(mockedData.fetch(Scope.WORKFLOW, "counter")).thenReturn(Optional.of(Integer.MAX_VALUE));

        assertThatThrownBy(
            () -> DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'counter'");

        verify(mockedData, never()).put(any(), any(), any());
    }

    @Test
    void testPerformIncrementsWithoutLostUpdatesWhenCalledConcurrently() throws Exception {
        int callCount = 3;

        Map<String, Object> storedValues = new ConcurrentHashMap<>();

        when(mockedData.fetch(Scope.WORKFLOW, "counter")).thenAnswer(
            invocation -> Optional.ofNullable(storedValues.get("counter")));
        when(mockedData.put(eq(Scope.WORKFLOW), eq("counter"), any())).thenAnswer(
            invocation -> {
                Thread.sleep(50);

                storedValues.put("counter", invocation.getArgument(2));

                return null;
            });

        CountDownLatch startLatch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(callCount);

        try {
            List<Future<Integer>> futures = new ArrayList<>();

            for (int index = 0; index < callCount; index++) {
                futures.add(executorService.submit(() -> {
                    startLatch.await();

                    return DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext);
                }));
            }

            startLatch.countDown();

            List<Integer> results = new ArrayList<>();

            for (Future<Integer> future : futures) {
                results.add(future.get(5, TimeUnit.SECONDS));
            }

            assertThat(results).containsExactlyInAnyOrder(5, 10, 15);
            assertEquals(15, storedValues.get("counter"));
        } finally {
            executorService.shutdownNow();
        }
    }

    @Test
    void testPerformReleasesTheLockWhenItFails() throws Exception {
        when(mockedData.fetch(Scope.WORKFLOW, "counter")).thenReturn(Optional.of("text"));

        assertThrows(
            IllegalArgumentException.class,
            () -> DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext));

        when(mockedData.fetch(Scope.WORKFLOW, "counter")).thenReturn(Optional.empty());

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        try {
            Future<Integer> future = executorService.submit(
                () -> DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext));

            assertEquals(5, future.get(2, TimeUnit.SECONDS));
        } finally {
            executorService.shutdownNow();
        }
    }
}
