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

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Ivica Cardic
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
     *
     */
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
            CURRENT_EXECUTION("Current Execution"),
            WORKFLOW("Workflow"),
            INSTANCE("Instance"),
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
         * @param <T>
         * @param scope
         * @return
         */
        <T> Map<String, T> getAll(Data.Scope scope);

        /**
         * @param scope
         * @param key
         * @param data
         */
        Void setValue(Data.Scope scope, String key, Object data);

        /**
         * @param scope
         * @param key
         */
        Void deleteValue(Data.Scope scope, String key);
    }
}
