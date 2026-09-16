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

package com.bytechef.component.datatable.action;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ActionDefinition.PerformFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import com.bytechef.platform.component.definition.ActionContextAware;
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.configuration.service.DataTableService;
import com.bytechef.platform.data.table.domain.DataTableRef;
import com.bytechef.platform.data.table.domain.DataTableResolution;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
abstract class AbstractDataTableActionTest {

    protected static final String BASE_NAME = "conversations";
    protected static final long ENVIRONMENT_ID = 1;
    protected static final long DATA_TABLE_ID = 11;

    protected final DataTableRowService dataTableRowService = mock(DataTableRowService.class);
    protected final DataTableService dataTableService = mock(DataTableService.class);

    /**
     * Stubs the two lookups {@code DataTableUtils.resolveDataTable} makes -- the registry resolution that settles which
     * physical table a base name addresses, and the {@code listTables} scan that carries its column metadata -- and
     * returns the ref every row statement is then addressed with.
     *
     * <p>
     * One {@code listTables} stub, not one per call: a second {@code when} on the same arguments would silently replace
     * the first rather than add to it, which reads as a stub and behaves as an empty listing.
     */
    protected DataTableRef stubResolvedDataTable() {
        DataTableRef dataTableRef = new DataTableRef(BASE_NAME, ENVIRONMENT_ID);

        when(
            dataTableService.fetchDataTableResolution(BASE_NAME, ENVIRONMENT_ID))
                .thenReturn(Optional.of(new DataTableResolution(DATA_TABLE_ID, dataTableRef)));
        when(dataTableService.listTables(ENVIRONMENT_ID))
            .thenReturn(
                List.of(new DataTableInfo(DATA_TABLE_ID, BASE_NAME, null, List.of(), Instant.EPOCH)));

        return dataTableRef;
    }

    protected static Object perform(
        ModifiableActionDefinition actionDefinition, Map<String, Object> inputParameters) throws Exception {

        PerformFunction performFunction = (PerformFunction) actionDefinition.getPerform()
            .orElseThrow();

        ActionContext actionContext = mock(
            ActionContext.class, withSettings().extraInterfaces(ActionContextAware.class));

        when(((ActionContextAware) actionContext).getEnvironmentId()).thenReturn(ENVIRONMENT_ID);

        Parameters parameters = MockParametersFactory.create(inputParameters);

        return performFunction.apply(parameters, MockParametersFactory.create(Map.of()), actionContext);
    }
}
