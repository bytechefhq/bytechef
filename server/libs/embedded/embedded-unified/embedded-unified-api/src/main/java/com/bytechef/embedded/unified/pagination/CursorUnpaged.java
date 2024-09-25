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

import org.springframework.data.domain.Sort;

/**
 * {@link CursorPageable} implementation to represent the absence of pagination information.
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
