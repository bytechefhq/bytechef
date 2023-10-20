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

package com.integri.atlas.engine.core.binary;

import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class BinaryTest {

    @Test
    public void testOf() {
        Assertions
            .assertThat(Binary.of("/tmp/fileName.txt", "data"))
            .hasFieldOrPropertyWithValue("data", "data")
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "fileName.txt")
            .hasFieldOrPropertyWithValue("__type__", "BINARY");

        Assertions
            .assertThat(Binary.of("/tmp/fileName.txt", "data", Map.of("key", "value")))
            .hasFieldOrPropertyWithValue("data", "data")
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "fileName.txt")
            .hasFieldOrPropertyWithValue("key", "value")
            .hasFieldOrPropertyWithValue("__type__", "BINARY");
    }
}
