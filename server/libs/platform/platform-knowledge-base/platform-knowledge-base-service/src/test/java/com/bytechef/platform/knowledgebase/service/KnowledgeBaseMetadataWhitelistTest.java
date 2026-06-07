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
 * Pure-function tests for the {@code metadata_fields} whitelist applied at tag-flattening time. Phase 18 adds the
 * whitelist to {@code KnowledgeBaseSource} so high-cardinality sources (HubSpot contacts with 90+ properties) can
 * narrow which metadata fields become document tags.
 *
 * <p>
 * Six branches matter and are exercised here:
 * <ol>
 * <li>Null whitelist → all metadata keys flattened (MVP behavior preserved).</li>
 * <li>Empty whitelist {@code {}} → all metadata keys flattened (treated as "include all", not "exclude all").</li>
 * <li>Whitelist with no {@code fields} key → all metadata keys flattened.</li>
 * <li>Whitelist with empty {@code fields} array → all metadata keys flattened (NOT "drop everything").</li>
 * <li>Whitelist with non-empty {@code fields} → only listed keys flattened.</li>
 * <li>Whitelist with fields that don't appear in metadata → no error, no tag for the missing field.</li>
 * </ol>
 * The null-vs-empty-Set distinction inside {@link KnowledgeBaseDocumentServiceImpl#extractAllowedFields} is the
 * load-bearing invariant — an empty Set would silently drop every tag, an outcome the existing MVP behavior must never
 * produce by accident.
 *
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

        // Empty whitelist {} means "no whitelist configured" — fall back to MVP full-flatten. NOT "drop everything".
        assertThat(tags).containsExactlyInAnyOrder("kind=contact", "owner=alice");
    }

    @Test
    void testWhitelistWithoutFieldsKeyFlatensAllMetadata() {
        Map<String, Object> metadata = orderedMap("kind", "contact", "owner", "alice");
        Map<String, Object> whitelist = Map.of("description", "shaped wrong but harmless");

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, whitelist);

        // A whitelist payload without the expected "fields" key is treated as malformed → fall back to full-flatten
        // so a stray UI bug or wrong-shape JSONB never silently strips all tags.
        assertThat(tags).containsExactlyInAnyOrder("kind=contact", "owner=alice");
    }

    @Test
    void testWhitelistWithEmptyFieldsArrayFlatensAllMetadata() {
        Map<String, Object> metadata = orderedMap("kind", "contact", "owner", "alice");
        Map<String, Object> whitelist = Map.of("fields", List.of());

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, whitelist);

        // Empty fields array means the user intent is ambiguous; we mirror MVP behavior. A workspace that genuinely
        // wants to drop all metadata tags should set metadata_fields to a sentinel like {"fields": ["__none__"]} so
        // no real key matches — explicit, not implicit.
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

        // A field listed in the whitelist but absent from the incoming record produces no tag (no NPE, no empty=
        // tag). The whitelist is a filter, not a schema enforcer — readers vary their emitted keys across records.
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

        // Existing MVP contract: null value renders as empty string after the "=" — preserved under the whitelist.
        // Verifies the whitelist refactor didn't accidentally tighten the null-value contract.
        assertThat(tags).containsExactlyInAnyOrder("kind=contact", "owner=");
    }

    @Test
    void testNonStringFieldNamesInWhitelistAreCoercedViaToString() {
        Map<String, Object> metadata = orderedMap("123", "numeric-key", "name", "alice");
        Map<String, Object> whitelist = Map.of("fields", List.of(123, "name"));

        List<String> tags = KnowledgeBaseDocumentServiceImpl.applyWhitelistAndFlatten(metadata, whitelist);

        // The whitelist payload originates from JSONB and may carry non-String entries (e.g. Integer from a JSON
        // number); the helper coerces via toString() so the match still hits.
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
