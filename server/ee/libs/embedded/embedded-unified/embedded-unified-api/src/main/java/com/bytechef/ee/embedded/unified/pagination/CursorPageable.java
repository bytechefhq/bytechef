/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.pagination;

import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.util.Assert;

/**
 * Abstract interface for cursor pagination information.
 *
 * @version ee
 *
 * @author Davide Pedone
 * @since 1.1
 */
public interface CursorPageable {

    /**
     * Returns a {@link CursorPageable} instance representing no pagination setup.
     *
     * @return
     */
    static CursorPageable unpaged() {
        return CursorUnpaged.INSTANCE;
    }

    /**
     * Returns the number of items to be returned.
     *
     * @return the number of items of that page
     */
    int getSize();

    /**
     * Returns the sorting parameter.
     *
     * @return
     */
    String getSort();

    /**
     * Returns the direction parameter.
     *
     * @return
     */
    Sort.Direction getDirection();

    /**
     * Returns the continuationToken parameter.
     *
     * @return
     */
    String getContinuationToken();

    /**
     * Returns whether the current {@link CursorPageable} contains pagination information.
     *
     * @return
     */
    default boolean isPaged() {
        return true;
    }

    /**
     * Returns whether the current {@link CursorPageable} does not contain pagination information.
     *
     * @return
     */
    default boolean isUnpaged() {
        return !isPaged();
    }

    /**
     * Returns the current {@link Sort} or the given one if the current one is unsorted.
     *
     * @param sort must not be {@literal null}.
     * @return
     */
    default String getSortOr(String sort) {

        Assert.notNull(sort, "Fallback Sort must not be null!");

        return getSort() != null ? getSort() : sort;
    }

    /**
     * Returns an {@link Optional} so that it can easily be mapped on.
     *
     * @return
     */
    default Optional<CursorPageable> toOptional() {
        return isUnpaged() ? Optional.empty() : Optional.of(this);
    }

}
