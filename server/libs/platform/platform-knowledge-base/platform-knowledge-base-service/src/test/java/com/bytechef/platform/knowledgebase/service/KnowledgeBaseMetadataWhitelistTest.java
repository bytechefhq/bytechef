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

package com.bytechef.platform.knowledgebase.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * @author Ivica Cardic
 */
class KnowledgeBaseMetadataWhitelistTest {

    @Test
    void testNullWhitelistFlatensAllMetadata() {
        Map<String, Object> metadata = orderedMap("kind", "contact", "owner", "alice", "stage", "qualified");

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, null);

        assertThat(tags).containsExactlyInAnyOrder(
            "kind=contact", "owner=alice", "stage=qualified");
    }

    @Test
    void testEmptyWhitelistMapFlatensAllMetadata() {
        Map<String, Object> metadata = orderedMap("kind", "contact", "owner", "alice");

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, Map.of());

        assertThat(tags).containsExactlyInAnyOrder("kind=contact", "owner=alice");
    }

    @Test
    void testWhitelistWithoutFieldsKeyFlatensAllMetadata() {
        Map<String, Object> metadata = orderedMap("kind", "contact", "owner", "alice");
        Map<String, Object> whitelist = Map.of("description", "shaped wrong but harmless");

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, whitelist);

        assertThat(tags).containsExactlyInAnyOrder("kind=contact", "owner=alice");
    }

    @Test
    void testWhitelistWithEmptyFieldsArrayFlatensAllMetadata() {
        Map<String, Object> metadata = orderedMap("kind", "contact", "owner", "alice");
        Map<String, Object> whitelist = Map.of("fields", List.of());

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, whitelist);

        assertThat(tags).containsExactlyInAnyOrder("kind=contact", "owner=alice");
    }

    @Test
    void testWhitelistWithFieldsNarrowsTagsToListedKeys() {
        Map<String, Object> metadata = orderedMap("kind", "contact", "owner", "alice", "stage", "qualified");
        Map<String, Object> whitelist = Map.of("fields", List.of("kind", "stage"));

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, whitelist);

        assertThat(tags).containsExactlyInAnyOrder("kind=contact", "stage=qualified");
    }

    @Test
    void testWhitelistFieldsThatDoNotExistInMetadataAreSilentlyIgnored() {
        Map<String, Object> metadata = orderedMap("kind", "contact", "owner", "alice");
        Map<String, Object> whitelist = Map.of("fields", List.of("kind", "doesNotExist"));

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, whitelist);

        assertThat(tags).containsExactly("kind=contact");
    }

    @Test
    void testNullMetadataYieldsEmptyTagList() {
        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(
            null, Map.of("fields", List.of("kind")));

        assertThat(tags).isEmpty();
    }

    @Test
    void testEmptyMetadataYieldsEmptyTagList() {
        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(
            Map.of(), Map.of("fields", List.of("kind")));

        assertThat(tags).isEmpty();
    }

    @Test
    void testNullValueInMetadataProducesEmptyValueTag() {
        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("kind", "contact");
        metadata.put("owner", null);

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, null);

        assertThat(tags).containsExactlyInAnyOrder("kind=contact", "owner=");
    }

    @Test
    void testNonStringFieldNamesInWhitelistAreCoercedViaToString() {
        Map<String, Object> metadata = orderedMap("123", "numeric-key", "name", "alice");
        Map<String, Object> whitelist = Map.of("fields", List.of(123, "name"));

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, whitelist);

        assertThat(tags).containsExactlyInAnyOrder("123=numeric-key", "name=alice");
    }

    private static Map<String, Object> orderedMap(Object... keysAndValues) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (int i = 0; i < keysAndValues.length; i += 2) {
            map.put((String) keysAndValues[i], keysAndValues[i + 1]);
        }

        return map;
    }
}
