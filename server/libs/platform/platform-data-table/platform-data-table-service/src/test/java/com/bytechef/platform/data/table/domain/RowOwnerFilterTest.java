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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bytechef.platform.owner.Owner;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class RowOwnerFilterTest {

    @Test
    void testUnrestrictedCarriesNoOwner() {
        RowOwnerFilter rowOwnerFilter = RowOwnerFilter.unrestricted();

        assertTrue(rowOwnerFilter.isUnrestricted());
        assertEquals(Optional.empty(), rowOwnerFilter.owner());
    }

    @Test
    void testOwnedByCarriesTheOwner() {
        Owner owner = Owner.connectedUser(1055L);

        RowOwnerFilter rowOwnerFilter = RowOwnerFilter.ownedBy(owner);

        assertFalse(rowOwnerFilter.isUnrestricted());
        assertEquals(Optional.of(owner), rowOwnerFilter.owner());
    }

    @Test
    void testFromAnEmptyOwnerIsUnrestricted() {
        RowOwnerFilter rowOwnerFilter = RowOwnerFilter.from(Optional.empty());

        assertTrue(rowOwnerFilter.isUnrestricted());
    }

    @Test
    void testFromAPresentOwnerIsScoped() {
        Owner owner = Owner.connectedUser(1055L);

        RowOwnerFilter rowOwnerFilter = RowOwnerFilter.from(Optional.of(owner));

        assertEquals(Optional.of(owner), rowOwnerFilter.owner());
    }
}
