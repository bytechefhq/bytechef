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

import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.execution.domain.DataTableRow;
import com.bytechef.automation.data.table.execution.service.DataTableRowService;
import com.bytechef.component.datatable.util.DataTableUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Clear Table: Delete all records from a table
 *
 * @author Ivica Cardic
 */
public class DataTableClearTableAction {

    private final DataTableService dataTableService;
    private final DataTableRowService dataTableRowService;

    public final ModifiableActionDefinition actionDefinition;

    @SuppressFBWarnings("EI")
    public DataTableClearTableAction(DataTableService dataTableService, DataTableRowService dataTableRowService) {
        this.dataTableService = dataTableService;
        this.dataTableRowService = dataTableRowService;
        this.actionDefinition = build();
    }

    private ModifiableActionDefinition build() {
        return action("clearTable")
            .title("Clear Table")
            .description("Delete all records from a table")
            .properties(
                string(TABLE)
                    .label("Table")
                    .required(true)
                    .options(DataTableUtils.getActionTableOptions(dataTableService)))
            .output(
                outputSchema(
                    object()
                        .properties(integer("deletedCount"))))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String baseName = inputParameters.getRequiredString(TABLE);

        List<DataTableRow> dataTableRows = dataTableRowService.listRows(
            baseName, Integer.MAX_VALUE, 0, Objects.requireNonNull(actionContextAware.getEnvironmentId()));
        int count = 0;

        for (DataTableRow dataTableRow : dataTableRows) {
            if (dataTableRowService.deleteRow(baseName, dataTableRow.id(), actionContextAware.getEnvironmentId())) {
                count++;
            }
        }

        Map<String, Object> result = new HashMap<>();

        result.put("deletedCount", count);

        return result;
    }
}
