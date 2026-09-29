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

package com.bytechef.platform.connection.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class ConnectionDTOTest {

    @Test
    void testBuilderCopiesEveryComponent() {
        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .active(true)
            .authorizationParameters(Map.of("token", "value"))
            .baseUri("baseUri")
            .componentName("slack")
            .connectionParameters(Map.of("key", "value"))
            .connectionVersion(2)
            .createdBy("admin")
            .environmentId(1)
            .id(7L)
            .name("Slack")
            .parameters(Map.of("key", "value"))
            .tags(List.of())
            .version(3)
            .build();

        ConnectionDTO copiedConnectionDTO = ConnectionDTO.builder(connectionDTO)
            .build();

        assertThat(copiedConnectionDTO).isEqualTo(connectionDTO);
    }
}
