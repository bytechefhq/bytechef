/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.pagination;

import org.springframework.data.domain.Sort;

/**
 * {@link CursorPageable} implementation to represent the absence of pagination information.
 *
 * @version ee
 *
 * @author Davide Pedone
 * @since 1.1
 */
public enum CursorUnpaged implements CursorPageable {

    INSTANCE;

    @Override
    public int getSize() {
        return 20;
    }

    @Override
    public String getSort() {
        return null;
    }

    @Override
    public Sort.Direction getDirection() {
        return Sort.Direction.DESC;
    }

    @Override
    public String getContinuationToken() {
        return null;
    }
}
