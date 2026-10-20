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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
}
