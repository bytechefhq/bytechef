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

package com.bytechef.platform.data.table.domain;

import com.bytechef.platform.data.table.internal.PhysicalTableNaming;
import org.springframework.util.Assert;

/**
 * One physical data table, fully addressed: the environment it lives in and the registry id it is known by.
 *
 * <p>
 * This is the only way to name a physical table. Every DDL and DML statement takes a ref rather than a table name, so
 * no call site can hand over a name and leave the callee to work out the rest of the address.
 *
 * @author Ivica Cardic
 */
public record DataTableRef(long dataTableId, long environmentId) {

    public DataTableRef {
        Assert.isTrue(dataTableId > 0, "dataTableId must be positive");
        Assert.isTrue(environmentId >= 0, "environmentId must not be negative");
    }

    /**
     * The table this ref addresses: {@code dt_<environmentId>_<dataTableId>}.
     */
    public String physicalName() {
        return PhysicalTableNaming.buildPhysicalName(environmentId, dataTableId);
    }
}
