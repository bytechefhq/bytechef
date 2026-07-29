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

import java.util.Optional;

/**
 * Provides the execution context available to triggers, extending the general {@link Context} with access to persistent
 * data storage scoped to a workflow or account. It is passed to trigger functions so they can read and write state that
 * survives across trigger invocations (for example, poll cursors or webhook enable outputs).
 *
 * @author Ivica Cardic
 */
public interface TriggerContext extends Context {

    /**
     * Executes the given function against this context's persistent {@link Data} store and returns its result.
     *
     * @param dataFunction the function to apply to the {@link Data} store
     * @param <R>          the type of the value returned by the function
     * @return the result produced by {@code dataFunction}
     */
    <R> R data(ContextFunction<Data, R> dataFunction);

    /**
     * Provides scoped, persistent key-value storage that triggers can use to retain state between invocations.
     */
    interface Data {

        /**
         * Defines the visibility scope within which stored data is isolated: bound to a single workflow, or shared
         * across the entire account.
         */
        enum Scope {
            WORKFLOW("Workflow"),
            ACCOUNT("Account");

            private final String label;

            Scope(String label) {
                this.label = label;
            }

            /**
             * Returns the human-readable label associated with this scope.
             *
             * @return the display label of the scope
             */
            public String getLabel() {
                return label;
            }
        }

        /**
         * Fetches the value stored under the given key within the given scope, if present.
         *
         * @param scope the scope to read from
         * @param key   the storage key
         * @param <T>   the type of the stored value
         * @return an {@link Optional} containing the stored value, or an empty {@link Optional} if no value is stored
         */
        <T> Optional<T> fetch(Data.Scope scope, String key);

        /**
         * Returns the value stored under the given key within the given scope.
         *
         * @param scope the scope to read from
         * @param key   the storage key
         * @param <T>   the type of the stored value
         * @return the stored value, or {@code null} if no value is stored
         */
        <T> T get(Data.Scope scope, String key);

        /**
         * Stores a value under the given key within the given scope, replacing any existing value.
         *
         * @param scope the scope to write to
         * @param key   the storage key
         * @param data  the value to store
         * @return always {@code null}; the return type exists so the call can be used within a {@link ContextFunction}
         */
        Void put(Data.Scope scope, String key, Object data);

        /**
         * Removes the value stored under the given key within the given scope, if any.
         *
         * @param scope the scope to remove from
         * @param key   the storage key
         * @return always {@code null}; the return type exists so the call can be used within a {@link ContextFunction}
         */
        Void remove(Data.Scope scope, String key);
    }
}
