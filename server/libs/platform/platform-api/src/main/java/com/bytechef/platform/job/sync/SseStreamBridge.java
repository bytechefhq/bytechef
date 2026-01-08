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

package com.bytechef.platform.job.sync;

/**
 * Represents a functional interface that serves as a stream bridge for handling event payloads, completion events, and
 * errors during the streaming process. This interface provides a mechanism to manage the lifecycle of streaming events,
 * including processing payloads, handling errors, and cleanup after stream completion.
 *
 * @author Ivica Cardic
 */
@FunctionalInterface
public interface SseStreamBridge {

    /**
     * Handles an event payload forwarded to the stream bridge. This method is invoked to process payloads streamed as
     * part of the event lifecycle.
     *
     * @param payload the payload object to be handled by the stream bridge. It represents the data associated with a
     *                particular event and should not be null.
     */
    void onEvent(Object payload);

    /**
     * Invoked to signal the completion of the stream. This method is a lifecycle callback intended to handle any
     * necessary cleanup or finalization steps after the stream has ended. <br/>
     * It is typically called when the stream successfully completes without any errors or interruptions. The default
     * implementation is a no-op and may be overridden by implementations of {@code SseStreamBridge} to provide specific
     * behavior upon stream completion.
     */
    default void onComplete() {
    }

    /**
     * Invoked when an error occurs during the streaming process. This method serves as a lifecycle callback that allows
     * handling of exceptions or errors that may arise while processing the stream. <br/>
     * Implementations of this method are expected to provide custom error-handling logic such as logging the error,
     * propagating it, or performing any necessary cleanup operations.
     *
     * @param throwable the {@code Throwable} instance representing the error encountered. It provides details about the
     *                  nature of the failure and should not be {@code null}.
     */
    default void onError(Throwable throwable) {
    }
}
