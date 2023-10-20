/*
 * Copyright 2021 <your company/name>.
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
 */

package com.integri.atlas.json.item;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

/**
 * @author Ivica Cardic
 */
public class BinaryItemTest {

    @Test
    public void testOf() {
        JSONAssert.assertEquals(
            new BinaryItem()
                .put("data", "data")
                .put("extension", "txt")
                .put("mimeType", "text/plain")
                .put("name", "fileName.txt"),
            BinaryItem.of("/tmp/fileName.txt", "data"),
            true
        );

        JSONAssert.assertEquals(
            new BinaryItem()
                .put("data", "data")
                .put("extension", "txt")
                .put("mimeType", "text/plain")
                .put("name", "fileName.txt")
                .put("key", "value"),
            BinaryItem.of("/tmp/fileName.txt", "data", Map.of("key", "value")),
            true
        );
    }
}
