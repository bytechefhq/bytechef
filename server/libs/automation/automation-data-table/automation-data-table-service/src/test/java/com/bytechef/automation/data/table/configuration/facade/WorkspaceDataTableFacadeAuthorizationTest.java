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

    private static void assertExpression(String methodName, String expression) {
        Method method = null;

        for (Method candidate : WorkspaceDataTableFacadeImpl.class.getDeclaredMethods()) {
            if (candidate.getName()
                .equals(methodName)) {
                method = candidate;

                break;
            }
        }

        assertThat(method)
            .as("method %s", methodName)
            .isNotNull();

        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize)
            .as("@PreAuthorize on %s", methodName)
            .isNotNull();
        assertThat(preAuthorize.value()).isEqualTo(expression);
    }
}
