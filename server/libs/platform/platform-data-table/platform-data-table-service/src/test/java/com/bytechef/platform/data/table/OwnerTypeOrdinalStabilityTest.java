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

package com.bytechef.platform.data.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bytechef.platform.constant.OwnerType;
import com.bytechef.platform.data.table.configuration.domain.DataTable;
import org.junit.jupiter.api.Test;

/**
 * {@link OwnerType} is persisted as an INT ordinal, so reordering its values silently reinterprets every stored row.
 *
 * @author Ivica Cardic
 */
class OwnerTypeOrdinalStabilityTest {

    @Test
    void testConnectedUserKeepsOrdinalZero() {
        assertEquals(0, OwnerType.CONNECTED_USER.ordinal());
    }

    @Test
    void testNoValueWasInsertedBeforeConnectedUser() {
        OwnerType[] ownerTypes = OwnerType.values();

        assertEquals("CONNECTED_USER", ownerTypes[0].name());
    }

    @Test
    void testDataTableOwnerTypeRoundTrips() {
        DataTable dataTable = new DataTable();

        dataTable.setOwnerType(OwnerType.CONNECTED_USER);

        assertEquals(OwnerType.CONNECTED_USER, dataTable.getOwnerType());
    }

    @Test
    void testDataTableOwnerDefaultsToNull() {
        DataTable dataTable = new DataTable();

        assertNull(dataTable.getOwnerType());
        assertNull(dataTable.getOwnerId());
    }
}
