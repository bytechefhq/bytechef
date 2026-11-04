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
import static com.bytechef.component.data.storage.constant.DataStorageConstants.TYPE;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.component.data.storage.constant.ValueType;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataStorageAppendValueToListActionTest {

    private final ActionContext mockedActionContext = mock(ActionContext.class);
    private final Data mockedData = mock(Data.class);
    private final AtomicReference<Object> storedValue = new AtomicReference<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void beforeEach() {
        when(mockedActionContext.data(any())).thenAnswer(
            invocation -> {
                ContextFunction<Data, ?> dataFunction = invocation.getArgument(0);

                return dataFunction.apply(mockedData);
            });

        when(mockedData.update(eq(Scope.WORKFLOW), eq("items"), any())).thenAnswer(
            invocation -> {
                Function<Optional<Object>, Object> updater = invocation.getArgument(2);

                Object result = updater.apply(Optional.ofNullable(storedValue.get()));

                storedValue.set(result);

                return result;
            });
    }

    @Test
    void testPerformAppendsToTheCurrentList() {
        List<Object> currentList = new ArrayList<>(List.of(1, 2));

        storedValue.set(currentList);

        Parameters parameters = MockParametersFactory.create(
            Map.of(KEY, "items", SCOPE, "WORKFLOW", TYPE, ValueType.INTEGER.name(), VALUE, 3));

        DataStorageAppendValueToListAction.perform(parameters, null, mockedActionContext);

        assertThat(storedValue.get()).isEqualTo(List.of(1, 2, 3));
        assertThat(currentList).containsExactly(1, 2);

        verifyNoFetchOrPut();
    }

    @Test
    void testPerformAppendsEachItemOfAList() {
        storedValue.set(new ArrayList<>(List.of("a")));

        Parameters parameters = MockParametersFactory.create(
            Map.of(KEY, "items", SCOPE, "WORKFLOW", TYPE, ValueType.ARRAY.name(), VALUE, List.of("b", "c")));

        DataStorageAppendValueToListAction.perform(parameters, null, mockedActionContext);

        assertThat(storedValue.get()).isEqualTo(List.of("a", "b", "c"));

        verifyNoFetchOrPut();
    }

    @Test
    void testPerformStartsAListWhenKeyIsMissing() {
        Parameters parameters = MockParametersFactory.create(
            Map.of(KEY, "items", SCOPE, "WORKFLOW", TYPE, ValueType.STRING.name(), VALUE, "a"));

        DataStorageAppendValueToListAction.perform(parameters, null, mockedActionContext);

        assertThat(storedValue.get()).isEqualTo(List.of("a"));

        verifyNoFetchOrPut();
    }

    private void verifyNoFetchOrPut() {
        verify(mockedData, never()).fetch(any(), any());
        verify(mockedData, never()).put(any(), any(), any());
    }
}
