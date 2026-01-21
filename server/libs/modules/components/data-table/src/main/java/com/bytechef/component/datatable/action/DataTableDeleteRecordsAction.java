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

import static com.bytechef.component.datatable.constant.DataTableConstants.IDS;
import static com.bytechef.component.datatable.constant.DataTableConstants.TABLE;
import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.execution.service.DataTableRowService;
import com.bytechef.component.datatable.util.DataTableUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.platform.component.definition.ActionContextAware;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Delete Record(s): Delete record(s) from a table
 *
 * @author Ivica Cardic
 */
public class DataTableDeleteRecordsAction {

    public final ModifiableActionDefinition actionDefinition;

    private final DataTableService dataTableService;
    private final DataTableRowService dataTableRowService;

    @SuppressFBWarnings("EI")
    public DataTableDeleteRecordsAction(DataTableService dataTableService, DataTableRowService dataTableRowService) {
        this.dataTableService = dataTableService;
        this.dataTableRowService = dataTableRowService;
        this.actionDefinition = build();
    }

    private ModifiableActionDefinition build() {
        return action("deleteRecords")
            .title("Delete Record(s)")
            .description("Delete record(s) from a table")
            .properties(
                string(TABLE)
                    .label("Table")
                    .required(true)
                    .options(DataTableUtils.getActionTableOptions(dataTableService)),
                array(IDS)
                    .label("Record IDs")
                    .description("IDs of records to delete")
                    .required(true)
                    .items(integer("id")))
            .output(
                outputSchema(
                    object()
                        .properties(
                            integer("deletedCount")
                                .label("Deleted Count"),
                            array("deletedIds")
                                .items(
                                    integer()))))
            .perform(this::perform);
    }

    @SuppressWarnings("PMD.UnusedFormalParameter")
    private Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext actionContext) {

        ActionContextAware actionContextAware = (ActionContextAware) actionContext;

        String baseName = inputParameters.getRequiredString(TABLE);

        Object[] ids = inputParameters.getRequiredArray(IDS);
        List<Long> deletedIds = new ArrayList<>();

        for (Object curId : ids) {
            long id = (curId instanceof Number number) ? number.longValue() : Long.parseLong(String.valueOf(curId));

            if (dataTableRowService.deleteRow(
                baseName, id, Objects.requireNonNull(actionContextAware.getEnvironmentId()))) {

                deletedIds.add(id);
            }
        }

        Map<String, Object> result = new HashMap<>();

        result.put("deletedCount", deletedIds.size());
        result.put("deletedIds", deletedIds);

        return result;
    }
}
