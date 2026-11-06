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

package com.bytechef.automation.data.table.configuration.facade;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.platform.data.table.execution.domain.DataTableRowConditions;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * @author Ivica Cardic
 */
class WorkspaceDataTableFacadeTest {

    private static final Set<String> UNGUARDED_METHOD_NAMES = Set.of("duplicateTable");

    @Test
    void testEveryFacadeMethodIsGuarded() {
        List<String> unguardedMethodNames = Arrays.stream(WorkspaceDataTableFacade.class.getMethods())
            .map(Method::getName)
            .filter(methodName -> !UNGUARDED_METHOD_NAMES.contains(methodName))
            .filter(methodName -> Arrays.stream(WorkspaceDataTableFacadeImpl.class.getDeclaredMethods())
                .filter(method -> method.getName()
                    .equals(methodName))
                .anyMatch(method -> !method.isAnnotationPresent(PreAuthorize.class)))
            .toList();

        assertThat(unguardedMethodNames).isEmpty();
    }

    @Test
    void testRowOperationsRequireTheDataTableScopes() throws NoSuchMethodException {
        Method listRowsMethod = WorkspaceDataTableFacadeImpl.class.getMethod(
            "listRows", long.class, DataTableRowConditions.class,
            List.class, int.class, int.class, long.class);
        Method upsertRowMethod = WorkspaceDataTableFacadeImpl.class.getMethod(
            "upsertRow", long.class, String.class, Map.class, long.class);

        assertThat(listRowsMethod.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')");
        assertThat(upsertRowMethod.getAnnotation(PreAuthorize.class)
            .value()).isEqualTo("hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }
}
