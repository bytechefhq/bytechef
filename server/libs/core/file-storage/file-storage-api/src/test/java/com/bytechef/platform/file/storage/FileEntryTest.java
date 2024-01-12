/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.platform.file.storage;

import static com.bytechef.atlas.configuration.constant.WorkflowConstants.TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.bytechef.commons.util.MapUtils;
import com.bytechef.evaluator.Evaluator;
import com.bytechef.file.storage.domain.FileEntry;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import java.util.Map;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
public class FileEntryTest {

    @BeforeAll
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    public static void beforeAll() {
        class MapUtilsMock extends MapUtils {
            static {
                objectMapper = new ObjectMapper();
            }
        }

        new MapUtilsMock();
    }

    @Test
    public void testOf1() {
        Assertions.assertThat(new FileEntry("fileName.txt", "base64:///tmp/fileName.txt"))
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "fileName.txt")
            .hasFieldOrPropertyWithValue("url", "base64:///tmp/fileName.txt");
    }

    @Test
    public void testOf2() {
        Assertions.assertThat(new FileEntry("name.txt", "base64:///tmp/fileName.txt"))
            .hasFieldOrPropertyWithValue("extension", "txt")
            .hasFieldOrPropertyWithValue("mimeType", "text/plain")
            .hasFieldOrPropertyWithValue("name", "name.txt")
            .hasFieldOrPropertyWithValue("url", "base64:///tmp/fileName.txt");
    }

    @Test
    public void testSpelEvaluation() {
        Map<String, Object> map = Evaluator.evaluate(
            Map.of(TYPE, "type", "result", "${fileEntry.name} ${fileEntry.url}"),
            Collections.singletonMap("fileEntry", new FileEntry("sample.txt", "base64:///tmp/fileName.txt")));

        assertEquals(
            "sample.txt base64:///tmp/fileName.txt", MapUtils.getString(map, "result"));
    }
}
