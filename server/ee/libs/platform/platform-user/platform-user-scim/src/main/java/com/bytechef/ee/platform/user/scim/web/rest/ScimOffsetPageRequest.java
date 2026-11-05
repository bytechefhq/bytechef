/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.user.scim.web.rest;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
record ScimOffsetPageRequest(long offset, int pageSize) implements Pageable {

    private static final Sort SORT_BY_ID = Sort.by(Sort.Direction.ASC, "id");

    ScimOffsetPageRequest {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }

        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be at least 1");
        }
    }

    static ScimOffsetPageRequest of(int startIndex, int count) {
        return new ScimOffsetPageRequest(startIndex - 1L, count);
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / pageSize);
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return SORT_BY_ID;
    }

    @Override
    public Pageable next() {
        return new ScimOffsetPageRequest(offset + pageSize, pageSize);
    }

    @Override
    public Pageable previousOrFirst() {
        return new ScimOffsetPageRequest(Math.max(0, offset - pageSize), pageSize);
    }

    @Override
    public Pageable first() {
        return new ScimOffsetPageRequest(0, pageSize);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new ScimOffsetPageRequest((long) pageNumber * pageSize, pageSize);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
