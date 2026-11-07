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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @version ee
 * @author Ivica Cardic
 */
class AiHubConfigurationDataTableFlatCrudToolCallbacksTest {

    private static final Set<String> EXPECTED_READ_NAMES =
        Set.of("listDataTables", "queryDataTable", "aggregateDataTable");

    private static final Set<String> EXPECTED_MUTATION_NAMES = Set.of(
        "addDataTableRow", "updateDataTableRow", "deleteDataTableRow", "addDataTableColumn", "createDataTable",
        "createDataTableFromCsv", "cloneDataTable", "dropDataTable");

    @Test
    void testReadModeReturnsExactlyTheReadNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.dataTableFlatCrudToolCallbacks(present(), false);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(EXPECTED_READ_NAMES);
    }

    @Test
    void testWriteModeReturnsTheReadAndMutationNames() {
        List<ToolCallback> toolCallbacks = AiHubConfiguration.dataTableFlatCrudToolCallbacks(present(), true);

        assertThat(toolCallbacks)
            .extracting(toolCallback -> toolCallback.getToolDefinition()
                .name())
            .containsExactlyInAnyOrderElementsOf(union(EXPECTED_READ_NAMES, EXPECTED_MUTATION_NAMES));
    }

    @Test
    void testAbsentFactoryReturnsEmptyListForBothModes() {
        assertThat(AiHubConfiguration.dataTableFlatCrudToolCallbacks(absent(), false)).isEmpty();
        assertThat(AiHubConfiguration.dataTableFlatCrudToolCallbacks(absent(), true)).isEmpty();
    }

    private static Set<String> union(Set<String> first, Set<String> second) {
        Set<String> union = new HashSet<>(first);

        union.addAll(second);

        return union;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableToolCallbacksFactory> present() {
        DataTableToolCallbacksFactory factory = new DataTableToolCallbacksFactory(mock(WorkspaceDataTableFacade.class));

        ObjectProvider<DataTableToolCallbacksFactory> provider = mock(ObjectProvider.class);

        when(provider.getIfAvailable()).thenReturn(factory);

        return provider;
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<DataTableToolCallbacksFactory> absent() {
        return mock(ObjectProvider.class);
    }
}
