/*
 * Copyright 2016-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Modifications copyright (C) 2021 <your company/name>
 */

package com.integri.atlas.json.item;

import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class JSONItemTest {

    @Test
    public void testOf() {
        assertEquals(new JSONItem().put("key", true), JSONItem.of("key", true), true);

        assertEquals(new JSONItem().put("key", 1), JSONItem.of("key", 1), true);

        assertEquals(new JSONItem().put("key", 2.1), JSONItem.of("key", 2.1), true);

        assertEquals(new JSONItem().put("key", 3.2F), JSONItem.of("key", 3.2F), true);

        assertEquals(new JSONItem().put("key", 2L), JSONItem.of("key", 2L), true);

        assertEquals(new JSONItem().put("key", "value"), JSONItem.of("key", "value"), true);

        assertEquals(
            new JSONItem().put("key", Map.of("key", "value")),
            JSONItem.of("key", Map.of("key", "value")),
            true
        );

        assertEquals(new JSONItem().put("key", List.of("value")), JSONItem.of("key", List.of("value")), true);

        assertEquals(
            new JSONItem().put("key", JSONItem.of("key", "value")),
            JSONItem.of("key", JSONItem.of("key", "value")),
            true
        );
    }
}
