/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.automation.app.proxy;

/**
 * The proxy could not store or read back one of its own temporary files. This is a local failure, never an upstream
 * one, so callers must not report it as an unavailable upstream even though its cause is an I/O error.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProxyStorageException extends RuntimeException {

    public ProxyStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
