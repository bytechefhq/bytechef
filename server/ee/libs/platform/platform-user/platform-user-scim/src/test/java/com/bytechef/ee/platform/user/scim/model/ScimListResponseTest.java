/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class ScimListResponseTest {

    private static final List<String> RESOURCES = List.of("a", "b", "c", "d", "e");

    @Test
    void testOfReportsTotalResultsIndependentlyOfPageSize() {
        ScimListResponse<String> scimListResponse = ScimListResponse.of(List.of("a", "b"), 42, 1);

        assertThat(scimListResponse.schemas()).containsExactly(ScimListResponse.SCHEMA);
        assertThat(scimListResponse.totalResults()).isEqualTo(42);
        assertThat(scimListResponse.itemsPerPage()).isEqualTo(2);
        assertThat(scimListResponse.startIndex()).isEqualTo(1);
        assertThat(scimListResponse.resources()).containsExactly("a", "b");
    }

    @Test
    void testSliceUsesOneBasedItemOffset() {
        ScimListResponse<String> scimListResponse = ScimListResponse.slice(RESOURCES, 2, 3);

        assertThat(scimListResponse.totalResults()).isEqualTo(5);
        assertThat(scimListResponse.startIndex()).isEqualTo(2);
        assertThat(scimListResponse.itemsPerPage()).isEqualTo(3);
        assertThat(scimListResponse.resources()).containsExactly("b", "c", "d");
    }

    @Test
    void testSliceWithCountZeroReturnsOnlyTotalResults() {
        ScimListResponse<String> scimListResponse = ScimListResponse.slice(RESOURCES, 1, 0);

        assertThat(scimListResponse.totalResults()).isEqualTo(5);
        assertThat(scimListResponse.itemsPerPage()).isZero();
        assertThat(scimListResponse.resources()).isEmpty();
    }

    @Test
    void testSliceTruncatesTheLastPage() {
        assertThat(ScimListResponse.slice(RESOURCES, 4, 100)
            .resources()).containsExactly("d", "e");
    }

    @Test
    void testSliceBeyondTheEndReturnsNoResources() {
        ScimListResponse<String> scimListResponse = ScimListResponse.slice(RESOURCES, 6, 10);

        assertThat(scimListResponse.totalResults()).isEqualTo(5);
        assertThat(scimListResponse.startIndex()).isEqualTo(6);
        assertThat(scimListResponse.resources()).isEmpty();
    }

    @Test
    void testSliceNormalizesStartIndexBelowOneAndNegativeCount() {
        assertThat(ScimListResponse.slice(RESOURCES, 0, 2)
            .resources()).containsExactly("a", "b");
        assertThat(ScimListResponse.slice(RESOURCES, -3, 1)
            .startIndex()).isEqualTo(1);
        assertThat(ScimListResponse.slice(RESOURCES, 1, -1)
            .resources()).isEmpty();
    }

    @Test
    void testSliceHandlesExtremeValuesWithoutOverflow() {
        assertThat(ScimListResponse.slice(RESOURCES, Integer.MAX_VALUE, Integer.MAX_VALUE)
            .resources()).isEmpty();
        assertThat(ScimListResponse.slice(RESOURCES, 1, Integer.MAX_VALUE)
            .resources()).hasSize(5);
    }
}
