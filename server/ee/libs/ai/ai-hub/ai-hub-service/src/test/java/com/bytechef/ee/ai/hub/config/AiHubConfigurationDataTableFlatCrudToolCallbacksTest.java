/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.automation.ai.tool.datatable.DataTableToolCallbacksFactory;
import com.bytechef.automation.data.table.configuration.facade.WorkspaceDataTableFacade;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Pins the three read names {@link AiHubConfiguration#dataTableFlatCrudToolCallbacks} restores flat on both hub agents,
 * and the eight mutation names {@link AiHubConfiguration#dataTableCatalogToolCallbacks} registers on the BUILD agent's
 * searchable tool catalog (ticket 732, Task 5 of the CRUD-delegate unwind), replacing the dissolved
 * {@code data_table_agent} delegate.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiHubConfigurationDataTableFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES = Set.of(
        "listDataTables", "queryDataTable", "aggregateDataTable");

    private static final Set<String> EXPECTED_MUTATION_NAMES = Set.of(
        "addDataTableRow", "updateDataTableRow", "deleteDataTableRow", "addDataTableColumn", "createDataTable",
        "createDataTableFromCsv", "cloneDataTable", "dropDataTable");

    @Test
    void testFlatCrudReturnsExactlyTheThreeReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.dataTableFlatCrudToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testFlatCrudReturnsEmptyListWhenFactoryAbsent() {
        assertThat(AiHubConfiguration.dataTableFlatCrudToolCallbacks(absent())).isEmpty();
    }

    @Test
    void testCatalogReturnsExactlyTheEightMutationNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.dataTableCatalogToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_MUTATION_NAMES);
    }

    @Test
    void testCatalogNeverIncludesAReadName() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.dataTableCatalogToolCallbacks(present());

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .doesNotContainAnyElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testCatalogReturnsEmptyListWhenFactoryAbsent() {
        assertThat(AiHubConfiguration.dataTableCatalogToolCallbacks(absent())).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableToolCallbacksFactory> present() {
        DataTableToolCallbacksFactory factory = new DataTableToolCallbacksFactory(
            mock(WorkspaceDataTableFacade.class), mock(DataTableService.class), mock(DataTableRowService.class),
            null);

        ObjectProvider<DataTableToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
