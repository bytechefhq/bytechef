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

package com.bytechef.platform.data.table.configuration.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.platform.owner.OwnerResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * @author Ivica Cardic
 */
class DataTableOwnerResolverGuardTest {

    @Test
    void testAMissingResolverFailsTheBoot() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class, () -> guard(null).checkOwnerResolverIsPresent());

        String message = exception.getMessage();

        // The operator has to be able to act on this without reading the code, so the message names both the module
        // that pulled the guard in and the fix.
        assertTrue(message.contains("data tables"), message);
        assertTrue(message.contains("embedded-configuration-service"), message);
    }

    @Test
    void testAPresentResolverStartsNormally() {
        assertDoesNotThrow(() -> guard(mock(OwnerResolver.class)).checkOwnerResolverIsPresent());
    }

    @SuppressWarnings("unchecked")
    private static DataTableOwnerResolverGuard guard(OwnerResolver ownerResolver) {
        ObjectProvider<OwnerResolver> ownerResolverProvider = mock(ObjectProvider.class);

        when(ownerResolverProvider.getIfAvailable()).thenReturn(ownerResolver);

        return new DataTableOwnerResolverGuard(ownerResolverProvider);
    }
}
