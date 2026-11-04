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

import static com.bytechef.component.data.storage.constant.DataStorageConstants.INDEX;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.KEY;
import static com.bytechef.component.data.storage.constant.DataStorageConstants.SCOPE;
import static org.assertj.core.api.Assertions.assertThat;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataStorageDeleteValueFromListActionTest {

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

        when(mockedData.fetch(Scope.WORKFLOW, "items")).thenAnswer(
            invocation -> Optional.ofNullable(storedValue.get()));

        when(mockedData.update(eq(Scope.WORKFLOW), eq("items"), any())).thenAnswer(
            invocation -> {
                Function<Optional<Object>, Object> updater = invocation.getArgument(2);

                Object result = updater.apply(Optional.ofNullable(storedValue.get()));

                storedValue.set(result);

                return result;
            });
    }

    @Test
    void testPerformRemovesTheItemAtTheIndex() {
        List<Object> currentList = new ArrayList<>(List.of(5, 6, 7));

        storedValue.set(currentList);

        DataStorageDeleteValueFromListAction.perform(createParameters(), null, mockedActionContext);

        assertThat(storedValue.get()).isEqualTo(List.of(5, 7));
        assertThat(currentList).containsExactly(5, 6, 7);

        verify(mockedData, never()).put(any(), any(), any());
    }

    @Test
    void testPerformLeavesAMissingKeyAlone() {
        Object result = DataStorageDeleteValueFromListAction.perform(createParameters(), null, mockedActionContext);

        assertThat(result).isNull();
        assertThat(storedValue.get()).isNull();

        verify(mockedData, never()).update(any(), any(), any());
        verify(mockedData, never()).put(any(), any(), any());
    }

    private static Parameters createParameters() {
        return MockParametersFactory.create(Map.of(INDEX, 1, KEY, "items", SCOPE, "WORKFLOW"));
    }
}
