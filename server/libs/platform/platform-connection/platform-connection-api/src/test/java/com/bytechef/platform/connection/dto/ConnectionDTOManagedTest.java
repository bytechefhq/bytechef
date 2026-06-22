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

import com.bytechef.platform.connection.domain.Connection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ConnectionDTOManagedTest {

    @Test
    void testManagedCopiedFromConnection() {
        Connection connection = new Connection();

        connection.setId(-1300L);
        connection.setComponentName("openAi");
        connection.setName("Open AI");
        connection.setManaged(true);

        ConnectionDTO dto =
            new ConnectionDTO(false, Map.of(), "https://api.openai.com", connection, Map.of(), List.of());

        assertThat(dto.managed()).isTrue();
    }

    @Test
    void testManagedDefaultsFalseViaBuilder() {
        ConnectionDTO dto = ConnectionDTO.builder()
            .componentName("slack")
            .name("Slack")
            .build();

        assertThat(dto.managed()).isFalse();
    }
}
