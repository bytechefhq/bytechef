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

package com.bytechef.automation.search.web.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.automation.search.SearchAssetType;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Every SearchAssetType a provider can return must exist in the GraphQL enum, otherwise a matching result fails
 * interface resolution at query time rather than at build time.
 *
 * @author Ivica Cardic
 */
class SearchAssetTypeSchemaParityTest {

    @Test
    void testEveryAssetTypeIsDeclaredInTheSchema() throws IOException {
        String schema = readSchema();

        List<String> missing = Arrays.stream(SearchAssetType.values())
            .map(Enum::name)
            .filter(name -> !schema.contains(name))
            .toList();

        assertThat(missing).isEmpty();
    }

    private String readSchema() throws IOException {
        try (InputStream inputStream = getClass().getResourceAsStream("/graphql/automation-search.graphqls")) {
            assertThat(inputStream).isNotNull();

            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
