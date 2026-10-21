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

package com.bytechef.component.definition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Configuration;
import com.bytechef.component.definition.Context.Http.Configuration.ConfigurationBuilder;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ContextHttpConfigurationTest {

    @Test
    void testMaxResponseBytesIsUnsetByDefault() {
        Configuration configuration = Configuration.newConfiguration()
            .build();

        assertNull(configuration.getMaxResponseBytes());
    }

    @Test
    void testMaxResponseBytesIsCarriedToTheConfiguration() {
        Configuration configuration = Http.responseType(Http.ResponseType.BINARY)
            .maxResponseBytes(1024)
            .build();

        assertEquals(Long.valueOf(1024), configuration.getMaxResponseBytes());
    }

    @Test
    void testBuildersDifferingOnlyInMaxResponseBytesAreNotEqual() {
        assertNotEquals(
            Configuration.newConfiguration()
                .maxResponseBytes(1),
            Configuration.newConfiguration()
                .maxResponseBytes(2));
        assertEquals(
            Configuration.newConfiguration()
                .maxResponseBytes(1),
            Configuration.newConfiguration()
                .maxResponseBytes(1));
    }

    @Test
    void testMaxResponseBytesBelowOneIsRefused() {
        ConfigurationBuilder configurationBuilder = Configuration.newConfiguration();

        assertThrows(IllegalArgumentException.class, () -> configurationBuilder.maxResponseBytes(0));
    }
}
