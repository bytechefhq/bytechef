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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataStorageAtomicIncrementActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Data mockedData = mock(Data.class);
    private final Parameters mockedParameters = MockParametersFactory.create(
        Map.of(KEY, "counter", SCOPE, "WORKFLOW", VALUE_TO_ADD, 5));
    private final AtomicReference<Object> storedValue = new AtomicReference<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        when(mockedActionContext.data(any())).thenAnswer(
            invocation -> {
                ContextFunction<Data, ?> dataFunction = invocation.getArgument(0);

                return dataFunction.apply(mockedData);
            });

        when(mockedData.update(eq(Scope.WORKFLOW), eq("counter"), any())).thenAnswer(
            invocation -> {
                Function<Optional<Object>, Object> updater = invocation.getArgument(2);

                Object result = updater.apply(Optional.ofNullable(storedValue.get()));

                storedValue.set(result);

                return result;
            });
    }

    @Test
    void testPerformStartsFromZeroWhenKeyIsMissing() {
        Integer result = DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext);

        assertThat(result).isEqualTo(5);
        assertThat(storedValue.get()).isEqualTo(5);
    }

    @Test
    void testPerformAddsToExistingValue() {
        storedValue.set(52L);

        Integer result = DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext);

        assertThat(result).isEqualTo(57);
        assertThat(storedValue.get()).isEqualTo(57);
    }

    @Test
    void testPerformAcceptsAnIntegerStoredValue() {
        storedValue.set(52);

        Integer result = DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext);

        assertThat(result).isEqualTo(57);
    }

    @Test
    void testPerformUsesUpdate() {
        DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext);

        verify(mockedData).update(eq(Scope.WORKFLOW), eq("counter"), any());
        verify(mockedData, never()).fetch(any(), any());
        verify(mockedData, never()).put(any(), any(), any());
    }

    @Test
    void testPerformRejectsNonNumericValue() {
        storedValue.set("text");

        assertThatThrownBy(
            () -> DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The value stored under key 'counter' is not a number");

        assertThat(storedValue.get()).isEqualTo("text");
    }

    @Test
    void testPerformRejectsStoredValueOutsideIntegerRange() {
        storedValue.set((long) Integer.MAX_VALUE + 1);

        assertThatThrownBy(
            () -> DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'counter'");

        assertThat(storedValue.get()).isEqualTo((long) Integer.MAX_VALUE + 1);
    }

    @Test
    void testPerformRejectsIncrementThatOverflows() {
        storedValue.set(Integer.MAX_VALUE);

        assertThatThrownBy(
            () -> DataStorageAtomicIncrementAction.perform(mockedParameters, null, mockedActionContext))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("'counter'");

        assertThat(storedValue.get()).isEqualTo(Integer.MAX_VALUE);
    }
}
