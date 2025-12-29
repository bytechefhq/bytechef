/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.pagination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.util.Assert;

/**
 * A slice of data restricted by the configured ContinuationToken
 *
 * @version ee
 *
 * @author Davide Pedone
 * @since 1.0
 */
public class CursorPageSlice<T> {

    private final List<T> content = new ArrayList<>();
    private final Boolean hasNext;
    private final String continuationToken;
    private final int size;

    /**
     * Creates a new {@link CursorPageSlice} with the given content and metadata
     *
     * @param content           must not be {@literal null}. from the current one.
     * @param size              the size of the {@link CursorPageSlice} to be returned.
     * @param continuationToken continuationToken to access the next {@link CursorPageSlice}. Can be {@literal null}.
     */
    public CursorPageSlice(List<T> content, int size, @Nullable String continuationToken) {
        Assert.notNull(content, "Content must not be null!");

        this.content.addAll(content);
        this.continuationToken = continuationToken;
        this.hasNext = continuationToken != null;
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CursorPageSlice<?> that)) {
            return false;
        }

        return size == that.size && Objects.equals(content, that.content) && Objects.equals(hasNext, that.hasNext) &&
            Objects.equals(continuationToken, that.continuationToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, hasNext, continuationToken, size);
    }

    /**
     * Returns the continuationToken to access the next {@link CursorPageSlice} whether there's one.
     *
     * @return Returns the continuationToken to access the next {@link CursorPageSlice} whether there's one.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * Returns the number of elements currently on this {@link CursorPageSlice}.
     *
     * @return the number of elements currently on this {@link CursorPageSlice}.
     */
    public int getNumberOfElements() {
        return content.size();
    }

    /**
     * Returns if there is a next {@link CursorPageSlice}.
     *
     * @return if there is a next {@link CursorPageSlice}.
     */
    public boolean hasNext() {
        return this.hasNext;
    }

    /**
     * Returns whether the {@link CursorPageSlice} has content at all.
     *
     * @return whether the {@link CursorPageSlice} has content at all.
     */
    public boolean hasContent() {
        return !content.isEmpty();
    }

    /**
     * Returns the page content as {@link List}.
     *
     * @return the page content as {@link List}.
     */
    public List<T> getContent() {
        return Collections.unmodifiableList(content);
    }

    /**
     * Returns the size of the {@link CursorPageSlice}.
     *
     * @return the size of the {@link CursorPageSlice}.
     */
    public int getSize() {
        return size;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<T> iterator() {
        return content.iterator();
    }

    public <U> CursorPageSlice<U> map(Function<? super T, ? extends U> converter) {
        return new CursorPageSlice<>(getConvertedContent(converter), size, continuationToken);
    }

    protected <U> List<U> getConvertedContent(Function<? super T, ? extends U> converter) {

        Assert.notNull(converter, "Function must not be null");

        return this.content.stream()
            .map(converter)
            .collect(Collectors.toList());
    }
}
