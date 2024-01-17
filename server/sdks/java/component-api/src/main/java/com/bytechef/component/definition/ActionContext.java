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

package com.bytechef.component.definition;

import java.util.Optional;
import java.util.function.Consumer;

/**
 *
 */
public interface ActionContext extends Context {

    /**
     * @param dataFunction
     * @param <R>
     * @return
     */
    <R> R data(ContextFunction<Data, R> dataFunction);

    /**
     * @param eventConsumer
     */
    void event(Consumer<Event> eventConsumer);

    /**
     * @param fileFunction
     * @param <R>
     * @return
     */
    <R> R file(ContextFunction<File, R> fileFunction);

    interface Event {
        /**
         * @param progress
         */
        void publishActionProgressEvent(int progress);
    }

    /**
     *
     */
    interface Data {

        enum Scope {
            ACCOUNT(4, "Account"),
            CURRENT_EXECUTION(1, "Current Execution"),
            WORKFLOW(2, "Workflow"),
            INSTANCE(3, "Instance");

            private final int id;
            private final String label;

            Scope(int id, String label) {
                this.id = id;
                this.label = label;
            }

            public static Data.Scope valueOf(int id) {
                return switch (id) {
                    case 1 -> Data.Scope.CURRENT_EXECUTION;
                    case 2 -> Data.Scope.WORKFLOW;
                    case 3 -> Data.Scope.INSTANCE;
                    case 4 -> Data.Scope.ACCOUNT;
                    default -> throw new IllegalStateException("Unexpected value: %s".formatted(id));
                };
            }

            public int getId() {
                return id;
            }

            public String getLabel() {
                return label;
            }
        }

        /**
         * @param <T>
         * @param scope
         * @param key
         * @return
         */
        <T> Optional<T> fetchValue(Data.Scope scope, String key);

        /**
         * @param <T>
         * @param scope
         * @param key
         * @return
         */
        <T> T getValue(Data.Scope scope, String key);

        /**
         * @param scope
         * @param key
         * @param data
         */
        void setValue(Data.Scope scope, String key, Object data);
    }

}
