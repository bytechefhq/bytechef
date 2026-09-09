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

package com.bytechef.platform.data.table.configuration.service;

import com.bytechef.definition.BaseProperty.ResourceType;
import com.bytechef.platform.data.table.execution.service.DataTableRowService;
import com.bytechef.platform.workflow.validator.ResourceReferenceResolver;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * @author Ivica Cardic
 */
@Component
public class DataTableReferenceResolver implements ResourceReferenceResolver {

    private final DataTableRowService dataTableRowService;
    private final DataTableService dataTableService;

    @SuppressFBWarnings("EI")
    public DataTableReferenceResolver(DataTableRowService dataTableRowService, DataTableService dataTableService) {
        this.dataTableRowService = dataTableRowService;
        this.dataTableService = dataTableService;
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.DATA_TABLE;
    }

    @Override
    @Nullable
    public String findProblem(String reference, long environmentId) {
        boolean exists = dataTableService.listTables(environmentId)
            .stream()
            .anyMatch(dataTableInfo -> reference.equalsIgnoreCase(dataTableInfo.baseName()));

        if (!exists) {
            return "Data table '" + reference + "' does not exist in this environment";
        }

        try {
            dataTableRowService.listRows(reference, 1, 0, environmentId);
        } catch (IllegalStateException e) {
            return e.getMessage();
        }

        return null;
    }
}
