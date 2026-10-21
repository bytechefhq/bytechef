/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.exception;

/**
 * Thrown when an HTTP response body is larger than the limit set with
 * {@code Http.Configuration.ConfigurationBuilder#maxResponseBytes(long)}. Before throwing, the client stops reading,
 * releases the connection and deletes whatever part of the body it had stored, so no more than the limit is ever
 * received and nothing of it is kept.
 *
 * @author Ivica Cardic
 */
public class HttpResponseTooLargeException extends RuntimeException {

    private final long maxResponseBytes;

    /**
     * Creates the exception for a body that passed the given limit.
     *
     * @param maxResponseBytes the limit, in bytes, the body passed
     */
    public HttpResponseTooLargeException(long maxResponseBytes) {
        super("HTTP response body exceeds %d bytes".formatted(maxResponseBytes));

        this.maxResponseBytes = maxResponseBytes;
    }

    /**
     * Returns the limit, in bytes, the body passed.
     *
     * @return the configured maximum response size
     */
    public long getMaxResponseBytes() {
        return maxResponseBytes;
    }
}
