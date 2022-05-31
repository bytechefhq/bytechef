/*
 * Copyright 2021 <your company/name>.
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

package com.integri.atlas.task.commons.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class MapUtilsTest {

    @Test
    public void testOf() {
        assertThat(MapUtils.of(List.of("value"))).hasFieldOrPropertyWithValue("column_1", "value");

        assertThat(MapUtils.of(List.of("value"), value -> value + 1)).hasFieldOrPropertyWithValue("column_1", "value1");

        assertThat(Map.of("key", "value")).hasFieldOrPropertyWithValue("key", "value");

        assertThat(MapUtils.of(Map.of("key", "value"), value -> value + 1))
            .hasFieldOrPropertyWithValue("key", "value1");
    }
}
