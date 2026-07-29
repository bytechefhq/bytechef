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

package com.bytechef.component.definition;

import com.bytechef.component.definition.ActionContext.Approval.Links;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Extends {@link Context} with facilities available while an action is executing, such as approval-link generation,
 * scoped data storage, progress-event publishing, and execution suspension for long-running or human-in-the-loop
 * actions.
 *
 * @author Ivica Cardic
 */
public interface ActionContext extends Context {

    /**
     * Returns trace id which originates from micro meter
     *
     * @return traceId
     */
    String getTraceId();

    /**
     * Applies the given function to an {@link Approval} instance, allowing the action to generate approval and
     * disapproval links used for human-in-the-loop confirmation flows.
     *
     * @param approvalFunction the function that produces the approval {@link Links} from an {@link Approval}
     * @return the generated approval and disapproval links
     */
    Links approval(ContextFunction<Approval, Links> approvalFunction);

    /**
     * Applies the given function to a {@link Data} instance, providing scoped access to persistent data storage during
     * action execution.
     *
     * @param dataFunction the function to apply to the {@link Data} utilities
     * @param <R>          the type of the result produced by the function
     * @return the result of applying the function
     */
    <R> R data(ContextFunction<Data, R> dataFunction);

    /**
     * Passes an {@link Event} instance to the given consumer, allowing the action to publish events such as execution
     * progress updates.
     *
     * @param eventConsumer the consumer that publishes events using the provided {@link Event} instance
     */
    void event(Consumer<Event> eventConsumer);

    /**
     * Suspends the current action execution. When called during a perform method, the framework will detect the
     * suspension after perform completes and handle it accordingly, regardless of what perform returns.
     *
     * @param suspend the suspension details including continuation parameters and expiration time
     */
    void suspend(Suspend suspend);

    /**
     * Provides generation of approval and disapproval links for human-in-the-loop approval flows.
     *
     * @deprecated approval handling is being superseded by the action suspension mechanism
     */
    @Deprecated
    interface Approval {
        /**
         * Generates a pair of approval and disapproval links that can be presented to a user to confirm or reject the
         * continuation of the action.
         *
         * @return the generated approval and disapproval {@link Links}
         */
        Links generateLinks();

        /**
         * Holds a pair of links used to approve or disapprove the continuation of an action.
         *
         * @param approvalLink    the link that approves continuation of the action
         * @param disapprovalLink the link that rejects continuation of the action
         */
        record Links(String approvalLink, String disapprovalLink) {
        }
    }

    /**
     * Provides publishing of events emitted while an action executes.
     */
    interface Event {
        /**
         * Publishes an action progress event conveying how far the action has advanced.
         *
         * @param progress the completion percentage of the action, expressed as a value between 0 and 100
         */
        void publishActionProgressEvent(int progress);
    }

    /**
     * Provides scoped, persistent key-value storage that actions can use to share and retain data across executions.
     */
    interface Data {

        enum Scope {
            CURRENT_EXECUTION("Current Execution"),
            WORKFLOW("Workflow"),
            PRINCIPAL("Principal"), // ProjectDeployment or IntegrationInstance
            ACCOUNT("Account");

            private final String label;

            Scope(String label) {
                this.label = label;
            }

            public String getLabel() {
                return label;
            }
        }

        /**
         * Fetches the value stored under the given key within the specified scope, if present.
         *
         * @param <T>   the expected type of the stored value
         * @param scope the scope in which to look up the value
         * @param key   the key identifying the value
         * @return an {@link Optional} containing the stored value, or an empty {@link Optional} if no value is present
         */
        <T> Optional<T> fetch(Data.Scope scope, String key);

        /**
         * Returns the value stored under the given key within the specified scope, or {@code null} if none is present.
         *
         * @param <T>   the expected type of the stored value
         * @param scope the scope in which to look up the value
         * @param key   the key identifying the value
         * @return the stored value, or {@code null} if no value is present
         */
        <T> T get(Data.Scope scope, String key);

        /**
         * Returns all key-value pairs stored within the specified scope.
         *
         * @param <T>   the expected type of the stored values
         * @param scope the scope whose entries are returned
         * @return a map of all keys to their stored values within the given scope
         */
        <T> Map<String, T> getAll(Data.Scope scope);

        /**
         * Stores the given data under the specified key within the given scope, overwriting any existing value.
         *
         * @param scope the scope in which to store the value
         * @param key   the key under which to store the value
         * @param data  the value to store
         * @return always {@code null}; the return type exists to allow use within functional expressions
         */
        Void put(Data.Scope scope, String key, Object data);

        /**
         * Removes the value stored under the given key within the specified scope.
         *
         * @param scope the scope from which to remove the value
         * @param key   the key identifying the value to remove
         * @return always {@code null}; the return type exists to allow use within functional expressions
         */
        Void remove(Data.Scope scope, String key);
    }

    /**
     * Represents a suspend state as part of an action execution. It indicates a temporary suspension with accompanying
     * parameters and expiration details.
     *
     * @param continueParameters a map containing parameters required to continue the suspended action
     * @param expiresAt          the timestamp indicating when the suspension expires
     */
    @SuppressFBWarnings("EI")
    record Suspend(Map<String, ?> continueParameters, Instant expiresAt) {
    }
}
