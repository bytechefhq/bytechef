/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.pagination;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.data.domain.Sort;

/**
 * Annotation to set defaults when injecting a {@link CursorPageable} into a controller method.
 *
 * @version ee
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
