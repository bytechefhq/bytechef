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

package com.bytechef.component.datastream.action.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class DataStreamStreamActionDefinitionTest {

    private static final String JOB_PARAMETERS_KEY = "__jobParameters";

    @Test
    void testExtractJobParameterOverridesReturnsEmptyWhenMetadataEmpty() throws Exception {
        Map<String, ?> result = invokeExtract(Map.of());

        assertThat(result).isEmpty();
    }

    @Test
    void testExtractJobParameterOverridesReturnsEmptyWhenReservedKeyMissing() throws Exception {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put("contextStoreSourceId", 42L);

        Map<String, ?> result = invokeExtract(metadata);

        assertThat(result).isEmpty();
    }

    @Test
    void testExtractJobParameterOverridesReturnsReservedMapWhenPresent() throws Exception {
        Map<String, Object> overrides = Map.of(
            "datastream.mode", "PARTIAL",
            "datastream.since", 1700000000000L);

        Map<String, Object> metadata = new HashMap<>();

        metadata.put(JOB_PARAMETERS_KEY, overrides);

        Map<String, ?> result = invokeExtract(metadata);

        assertThat(result).hasSize(2);
        assertThat(result.get("datastream.mode")).isEqualTo("PARTIAL");
        assertThat(result.get("datastream.since")).isEqualTo(1700000000000L);
    }

    @Test
    void testExtractJobParameterOverridesReturnsEmptyWhenReservedKeyValueIsNotMap() throws Exception {
        Map<String, Object> metadata = new HashMap<>();

        metadata.put(JOB_PARAMETERS_KEY, "not a map");

        Map<String, ?> result = invokeExtract(metadata);

        assertThat(result).isEmpty();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> invokeExtract(Map<String, Object> jobMetadata) throws Exception {
        Method method = DataStreamStreamActionDefinition.class.getDeclaredMethod(
            "extractJobParameterOverrides", Map.class);

        method.setAccessible(true);

        return (Map<String, ?>) method.invoke(null, jobMetadata);
    }
}
