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

package com.bytechef.ai.copilot.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class SampleOutputToolsTest {

    private final SampleOutputTools sampleOutputTools = new SampleOutputTools();

    @Test
    void testUpdateSampleOutputEchoesValidJson() {
        String result = sampleOutputTools.updateSampleOutput("{\"a\":1}");

        Map<String, ?> map = JsonUtils.readMap(result);

        assertThat(map).containsKey("sampleOutput");
        assertThat(map).doesNotContainKey("error");
    }

    @Test
    void testUpdateSampleOutputRejectsInvalidJson() {
        String result = sampleOutputTools.updateSampleOutput("not json");

        Map<String, ?> map = JsonUtils.readMap(result);

        assertThat(map).containsKey("error");
    }
}
