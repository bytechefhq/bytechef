/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.unified.exception;

import org.jspecify.annotations.Nullable;

/**
 * An exception indicating that a cursor pagination operation failed.
 *
 * @version ee
 *
 * @author Davide Pedone
 * @since 1.0
 */
public class CursorPaginationException extends RuntimeException {

    public CursorPaginationException(@Nullable String s) {
        super(s);
    }

    public CursorPaginationException(@Nullable String s, @Nullable Throwable throwable) {
        super(s, throwable);
    }

}
