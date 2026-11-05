/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * @version ee
 */
class ScimOffsetPageRequestTest {

    @Test
    void testOfConvertsOneBasedStartIndexToZeroBasedOffset() {
        ScimOffsetPageRequest scimOffsetPageRequest = ScimOffsetPageRequest.of(50, 100);

        assertThat(scimOffsetPageRequest.getOffset()).isEqualTo(49L);
        assertThat(scimOffsetPageRequest.getPageSize()).isEqualTo(100);
        assertThat(scimOffsetPageRequest.getPageNumber()).isZero();
        assertThat(scimOffsetPageRequest.hasPrevious()).isTrue();
        assertThat(scimOffsetPageRequest.isPaged()).isTrue();
    }

    @Test
    void testSortsByIdForStablePaging() {
        assertThat(ScimOffsetPageRequest.of(1, 10)
            .getSort()).isEqualTo(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    void testNavigationKeepsItemOffsets() {
        ScimOffsetPageRequest scimOffsetPageRequest = new ScimOffsetPageRequest(5, 10);

        Pageable next = scimOffsetPageRequest.next();
        Pageable previous = scimOffsetPageRequest.previousOrFirst();

        assertThat(next.getOffset()).isEqualTo(15L);
        assertThat(previous.getOffset()).isZero();
        assertThat(scimOffsetPageRequest.first()
            .getOffset()).isZero();
        assertThat(scimOffsetPageRequest.withPage(3)
            .getOffset()).isEqualTo(30L);
        assertThat(new ScimOffsetPageRequest(0, 10).hasPrevious()).isFalse();
    }

    @Test
    void testRejectsInvalidArguments() {
        assertThatThrownBy(() -> new ScimOffsetPageRequest(-1, 10)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScimOffsetPageRequest(0, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
