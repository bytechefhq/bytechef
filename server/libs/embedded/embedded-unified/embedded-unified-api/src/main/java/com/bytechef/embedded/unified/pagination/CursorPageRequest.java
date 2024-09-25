/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.embedded.unified.pagination;

import java.util.Objects;
import org.springframework.data.domain.Sort;

/**
 * Basic Java Bean implementation of {@code CursorPageable}.
 *
 * @author Davide Pedone
 * @since 1.1
 */
public class CursorPageRequest implements CursorPageable {

    private String continuationToken;
    private int size;
    private String sort;
    private Sort.Direction direction;

    protected CursorPageRequest() {
    }

    public CursorPageRequest(String continuationToken, int size, String sort, Sort.Direction direction) {
        this.continuationToken = continuationToken;
        this.size = size;
        this.sort = sort;
        this.direction = direction;
    }

    public static CursorPageRequest of(String continuationToken, int size) {
        CursorPageable unpaged = CursorPageable.unpaged();

        return of(continuationToken, size, null, unpaged.getDirection());
    }

    public static CursorPageRequest of(int size) {
        CursorPageable unpaged = CursorPageable.unpaged();

        return of(null, size, null, unpaged.getDirection());
    }

    public static CursorPageRequest of(int size, String sort, Sort.Direction direction) {
        return of(null, size, sort, direction);
    }

    public static CursorPageRequest of(String continuationToken, int size, String sort) {
        CursorPageable unpaged = CursorPageable.unpaged();

        return of(continuationToken, size, sort, unpaged.getDirection());
    }

    public static CursorPageRequest of(String continuationToken, int size, String sort, Sort.Direction direction) {
        return new CursorPageRequest(continuationToken, size, sort, direction);
    }

    @Override
    public String getContinuationToken() {
        return continuationToken;
    }

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public String getSort() {
        return sort;
    }

    @Override
    public Sort.Direction getDirection() {
        return direction;
    }

    public void setContinuationToken(String continuationToken) {
        this.continuationToken = continuationToken;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public void setDirection(Sort.Direction direction) {
        this.direction = direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof CursorPageRequest that)) {
            return false;
        }

        return size == that.size && Objects.equals(continuationToken, that.continuationToken) &&
            Objects.equals(sort, that.sort) && direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(continuationToken, size, sort, direction);
    }
}
