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

package com.bytechef.platform.connection.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiProviderConnectionIdTest {

    @Test
    void testEncodeIsNegativeAndNeverCollidesWithRealIds() {
        long id = AiProviderConnectionId.encode(13, 0);

        assertThat(id).isNegative();
        assertThat(id).isLessThan(-99L);
    }

    @Test
    void testRoundTrip() {
        long id = AiProviderConnectionId.encode(17, 2);

        assertThat(AiProviderConnectionId.isAiProviderConnectionId(id)).isTrue();
        assertThat(AiProviderConnectionId.providerId(id)).isEqualTo(17);
        assertThat(AiProviderConnectionId.environmentId(id)).isEqualTo(2);
    }

    @Test
    void testRealIdsAreNotAiProviderIds() {
        assertThat(AiProviderConnectionId.isAiProviderConnectionId(1050L)).isFalse();
        assertThat(AiProviderConnectionId.isAiProviderConnectionId(0L)).isFalse();
    }
}
