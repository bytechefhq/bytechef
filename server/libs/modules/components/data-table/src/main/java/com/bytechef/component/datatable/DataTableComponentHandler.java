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

package com.bytechef.component.datatable;

import static com.bytechef.component.datatable.constant.DataTableConstants.DATA_TABLE;
import static com.bytechef.component.definition.ComponentDsl.component;

import com.bytechef.automation.data.table.configuration.service.DataTableService;
import com.bytechef.automation.data.table.configuration.service.DataTableWebhookService;
import com.bytechef.automation.data.table.execution.service.DataTableRowService;
import com.bytechef.component.ComponentHandler;
import com.bytechef.component.datatable.action.DataTableClearTableAction;
import com.bytechef.component.datatable.action.DataTableCreateRecordsAction;
import com.bytechef.component.datatable.action.DataTableDeleteRecordsAction;
import com.bytechef.component.datatable.action.DataTableFindRecordsAction;
import com.bytechef.component.datatable.action.DataTableGetRecordAction;
import com.bytechef.component.datatable.action.DataTableUpdateRecordAction;
import com.bytechef.component.datatable.trigger.DataTableRecordCreatedTrigger;
import com.bytechef.component.datatable.trigger.DataTableRecordDeletedTrigger;
import com.bytechef.component.datatable.trigger.DataTableRecordUpdatedTrigger;
import com.bytechef.component.definition.ComponentCategory;
import com.bytechef.component.definition.ComponentDefinition;
import com.bytechef.platform.component.definition.AbstractComponentDefinitionWrapper;
import org.springframework.stereotype.Component;

/**
 * Data Table component providing actions and triggers for table row events.
 *
 * @author Ivica Cardic
 */
@Component(DATA_TABLE + "_v1_ComponentHandler")
public class DataTableComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition;

    public DataTableComponentHandler(
        DataTableService dataTableService, DataTableRowService dataTableRowService,
        DataTableWebhookService dataTableWebhookService) {

        this.componentDefinition = new DataTableComponentDefinitionImpl(
            dataTableService, dataTableRowService, dataTableWebhookService);
    }

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    private static class DataTableComponentDefinitionImpl extends AbstractComponentDefinitionWrapper {

        public DataTableComponentDefinitionImpl(
            DataTableService dataTableService, DataTableRowService dataTableRowService,
            DataTableWebhookService dataTableWebhookService) {

            super(component(DATA_TABLE)
                .title("Data Table")
                .description("Work with ByteChef Data Tables and react to row changes.")
                .icon("path:assets/data-table.svg")
                .categories(ComponentCategory.HELPERS)
                .actions(
                    new DataTableCreateRecordsAction(dataTableService, dataTableRowService).actionDefinition,
                    new DataTableDeleteRecordsAction(dataTableService, dataTableRowService).actionDefinition,
                    new DataTableUpdateRecordAction(dataTableService, dataTableRowService).actionDefinition,
                    new DataTableGetRecordAction(dataTableService, dataTableRowService).actionDefinition,
                    new DataTableFindRecordsAction(dataTableService, dataTableRowService).actionDefinition,
                    new DataTableClearTableAction(dataTableService, dataTableRowService).actionDefinition)
                .triggers(
                    new DataTableRecordCreatedTrigger(
                        dataTableRowService, dataTableService, dataTableWebhookService).triggerDefinition,
                    new DataTableRecordUpdatedTrigger(
                        dataTableRowService, dataTableService, dataTableWebhookService).triggerDefinition,
                    new DataTableRecordDeletedTrigger(
                        dataTableRowService, dataTableService, dataTableWebhookService).triggerDefinition));
        }
    }
}
