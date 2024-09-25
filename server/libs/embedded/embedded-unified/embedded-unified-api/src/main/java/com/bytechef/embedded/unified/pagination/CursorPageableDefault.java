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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.data.domain.Sort;

/**
 * Annotation to set defaults when injecting a {@link CursorPageable} into a controller method.
 *
 * @author Davide Pedone
 * @since 1.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface CursorPageableDefault {

    /**
     * Alias for {@link #size()}. Prefer to use the {@link #size()} method as it makes the annotation declaration more
     * expressive.
     *
     * @return
     */
    int value() default 20;

    /**
     * The default-size the injected {@link CursorPageable} should get if no corresponding parameter defined in request
     * (default is 20).
     */
    int size() default 20;

    String sort() default "";

    String continuationToken() default "";

    /**
     * The direction to sort by. Defaults to {@link Sort.Direction#DESC}.
     *
     * @return
     */
    Sort.Direction direction() default Sort.Direction.DESC;
}
