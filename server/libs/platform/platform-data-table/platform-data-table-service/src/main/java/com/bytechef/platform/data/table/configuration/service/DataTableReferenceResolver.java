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
import com.bytechef.platform.data.table.configuration.domain.DataTableInfo;
import com.bytechef.platform.data.table.domain.DataTableRef;
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
        DataTableInfo dataTableInfo = findTable(reference, environmentId);

        if (dataTableInfo == null) {
            return "Data table '" + reference + "' does not exist in this environment";
        }

        return findRowProblem(dataTableInfo, environmentId);
    }

    private @Nullable String findRowProblem(DataTableInfo dataTableInfo, long environmentId) {

        try {
            // The base name comes from the table that was found rather than from the reference, so the ref is well
            // formed by construction -- a DataTableRef validates its base name, and a reference typed into a workflow
            // need not be a legal identifier at all.
            dataTableRowService.listRows(
                new DataTableRef(dataTableInfo.baseName(), environmentId), 1, 0);
        } catch (IllegalStateException illegalStateException) {
            return illegalStateException.getMessage();
        }

        return null;
    }

    private @Nullable DataTableInfo findTable(String reference, long environmentId) {
        return dataTableService.listTables(environmentId)
            .stream()
            .filter(dataTableInfo -> reference.equalsIgnoreCase(dataTableInfo.baseName()))
            .findFirst()
            .orElse(null);
    }
}
