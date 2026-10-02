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

package com.bytechef.platform.ai.sensitivedata.tokenization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
class PiiTokenSessionToolContextTest {

    @Test
    void testSessionRoundTripsThroughToolContext() {
        PiiTokenSession session = PiiTokenSession.create();

        Map<String, Object> context = PiiTokenSessionToolContext.into(Map.of("existing", "kept"), session);

        assertThat(context).containsEntry("existing", "kept");
        assertThat(PiiTokenSessionToolContext.from(new ToolContext(context))).isSameAs(session);
    }

    @Test
    void testAbsentSessionReadsAsNull() {
        assertThat(PiiTokenSessionToolContext.from(new ToolContext(Map.of()))).isNull();
        assertThat(PiiTokenSessionToolContext.from(null)).isNull();
    }

    @Test
    void testIntoDoesNotMutateTheSuppliedMap() {
        Map<String, Object> original = Map.of("existing", "kept");
        PiiTokenSession session = PiiTokenSession.create();

        Map<String, Object> merged = PiiTokenSessionToolContext.into(original, session);

        assertThat(original).doesNotContainKey(PiiTokenSessionToolContext.KEY);
        assertThat(merged).containsKey(PiiTokenSessionToolContext.KEY);
    }
}
