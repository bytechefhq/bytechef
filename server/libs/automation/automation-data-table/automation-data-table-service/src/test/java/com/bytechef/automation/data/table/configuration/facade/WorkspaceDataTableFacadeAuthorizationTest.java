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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Pins the {@code @PreAuthorize} expressions that workspace-scope data-table operations (T22), enforced at the facade
 * tier. Per-table DDL resolves the owning workspace via {@code DataTable:ResourceRole} (EDITOR); create/list take a
 * {@code workspaceId} argument.
 *
 * @author Ivica Cardic
 */
class WorkspaceDataTableFacadeAuthorizationTest {

    @Test
    void testCreateRequiresWorkspaceEditor() {
        assertExpression("createTable", "hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_CREATE')");
    }

    @Test
    void testListRequiresWorkspaceViewer() {
        assertExpression("listTables", "hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_VIEW')");
    }

    @Test
    void testGetDataTableTagsRequiresWorkspaceViewer() {
        assertExpression("getDataTableTags", "hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_VIEW')");
    }

    @Test
    void testAddColumnRequiresTableEditor() {
        assertExpression("addColumn", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testDropRequiresTableEditor() {
        assertExpression("dropTable", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testDuplicateRequiresTableEditor() {
        assertExpression("duplicateTable", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testRemoveColumnRequiresTableEditor() {
        assertExpression("removeColumn", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testRenameColumnRequiresTableEditor() {
        assertExpression("renameColumn", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testRenameTableRequiresTableEditor() {
        assertExpression("renameTable", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testListRowsRequiresTableViewer() {
        assertExpression("listRows", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')");
    }

    @Test
    void testInsertRowRequiresTableEditor() {
        assertExpression("insertRow", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testUpdateRowRequiresTableEditor() {
        assertExpression("updateRow", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testDeleteRowRequiresTableEditor() {
        assertExpression("deleteRow", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testExportCsvRequiresTableViewer() {
        assertExpression("exportCsv", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')");
    }

    @Test
    void testImportCsvRequiresTableEditor() {
        assertExpression("importCsv", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testUpdateTagsRequiresTableEditor() {
        assertExpression("updateTags", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testListWebhooksRequiresTableViewer() {
        assertExpression("listWebhooks", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')");
    }

    @Test
    void testGetTableRequiresTableViewer() {
        assertExpression("getTable", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')");
    }

    @Test
    void testUpdateDescriptionRequiresTableEditor() {
        assertExpression("updateDescription", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testListRowsOverloadsBothRequireTableViewer() {
        assertExpression("listRows", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')");
    }

    @Test
    void testGetRowRequiresTableViewer() {
        assertExpression("getRow", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')");
    }

    @Test
    void testFetchRowByExternalIdRequiresTableViewer() {
        assertExpression("fetchRowByExternalId", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')");
    }

    @Test
    void testInsertRowOverloadsBothRequireTableEditor() {
        assertExpression("insertRow", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testUpdateRowOverloadsBothRequireTableEditor() {
        assertExpression("updateRow", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testUpsertRowRequiresTableEditor() {
        assertExpression("upsertRow", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testDeleteRowByExternalIdRequiresTableEditor() {
        assertExpression("deleteRowByExternalId", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testInsertRowsRequiresTableEditor() {
        assertExpression("insertRows", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testDeleteRowsRequiresTableEditor() {
        assertExpression("deleteRows", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testClearRowsRequiresTableEditor() {
        assertExpression("clearRows", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_EDIT')");
    }

    @Test
    void testGetTagsByTableIdRequiresWorkspaceViewer() {
        assertExpression("getTagsByTableId", "hasPermission(#workspaceId, 'Workspace', 'DATA_TABLE_VIEW')");
    }

    @Test
    void testGetWorkspaceIdRequiresTableViewer() {
        assertExpression("getWorkspaceId", "hasPermission(#dataTableId, 'DataTable', 'DATA_TABLE_VIEW')");
    }

    /**
     * Asserts the expression on EVERY declared method with this name, not just the first one {@code getDeclaredMethods}
     * happens to return -- {@code getDeclaredMethods} order is unspecified, and several of these names are overloaded
     * (e.g. {@code listRows}, {@code insertRow}, {@code updateRow}), so checking only one match would silently skip the
     * other overload's guard.
     */
    private static void assertExpression(String methodName, String expression) {
        List<Method> methods = new ArrayList<>();

        for (Method candidate : WorkspaceDataTableFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {
                methods.add(candidate);
            }
        }

        assertThat(methods)
            .as("method %s", methodName)
            .isNotEmpty();

        for (Method method : methods) {
            PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

            assertThat(preAuthorize)
                .as("@PreAuthorize on %s%s", methodName, Arrays.toString(method.getParameterTypes()))
                .isNotNull();
            assertThat(preAuthorize.value())
                .as("@PreAuthorize value on %s%s", methodName, Arrays.toString(method.getParameterTypes()))
                .isEqualTo(expression);
        }
    }
}
