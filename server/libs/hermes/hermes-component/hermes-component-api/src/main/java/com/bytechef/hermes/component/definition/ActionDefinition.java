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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.component.exception.ComponentExecutionException;
import com.bytechef.hermes.definition.Help;
import com.bytechef.hermes.definition.Property.InputProperty;
import com.bytechef.hermes.definition.Property.OutputProperty;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author Ivica Cardic
 */
public interface ActionDefinition {

    /**
     * TODO
     *
     * @return
     */
    Optional<Boolean> getBatch();

    /**
     * TODO
     *
     * @return
     */
    Optional<Boolean> getDeprecated();

    /**
     *
     * @return
     */
    Optional<String> getComponentDescription();

    /**
     *
     * @return
     */
    String getComponentName();

    /**
     *
     * @return
     */
    Optional<String> getComponentTitle();

    /**
     *
     * @return
     */
    int getComponentVersion();

    /**
     *
     * @return
     */
    Optional<String> getDescription();

    /**
     *
     * @return
     */
    Optional<EditorDescriptionDataSource> getEditorDescriptionDataSource();

    /**
     * The code that should be executed when an action runs as a task inside the workflow engine.
     *
     * @return an optional execute function implementation
     */
    Optional<PerformFunction> getPerform();

    /**
     *
     * @return
     */
    Optional<Help> getHelp();

    /**
     *
     * @return
     */
    Optional<Map<String, Object>> getMetadata();

    /**
     *
     * @return
     */
    String getName();

    /**
     *
     * @return
     */
    Optional<OutputProperty<?>> getOutputSchema();

    /**
     *
     * @return
     */
    Optional<OutputSchemaDataSource> getOutputSchemaDataSource();

    /**
     *
     * @return
     */
    Optional<List<? extends InputProperty>> getProperties();

    /**
     *
     * @return
     */
    Optional<Object> getSampleOutput();

    /**
     *
     * @return
     */
    Optional<SampleOutputDataSource> getSampleOutputDataSource();

    /**
     *
     * @return
     */
    Optional<String> getTitle();

    /**
     *
     */
    @FunctionalInterface
    interface PerformFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param context
         * @return
         */
        Object apply(ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context)
            throws ComponentExecutionException;
    }

    /**
     *
     */
    interface ActionContext extends Context {

        /**
         *
         * @param dataFunction
         * @return
         * @param <R>
         */
        <R> R data(ContextFunction<Data, R> dataFunction);

        /**
         *
         * @param eventConsumer
         */
        void event(Consumer<Event> eventConsumer);

        /**
         *
         * @param fileFunction
         * @return
         * @param <R>
         */
        <R> R file(ContextFunction<File, R> fileFunction);

        interface Event {
            /**
             *
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

                public static Scope valueOf(int id) {
                    return switch (id) {
                        case 1 -> Scope.CURRENT_EXECUTION;
                        case 2 -> Scope.WORKFLOW;
                        case 3 -> Scope.INSTANCE;
                        case 4 -> Scope.ACCOUNT;
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
            <T> Optional<T> fetchValue(Scope scope, String key);

            /**
             * @param <T>
             * @param scope
             * @param key
             * @return
             */
            <T> T getValue(Scope scope, String key);

            /**
             * @param scope
             * @param key
             * @param data
             */
            void setValue(Scope scope, String key, Object data);
        }

        /**
         *
         */
        interface FileEntry {

            /**
             *
             * @return
             */
            String getExtension();

            /**
             *
             * @return
             */
            String getMimeType();

            /**
             *
             * @return
             */
            String getName();

            /**
             *
             * @return
             */
            String getUrl();
        }
    }
}
