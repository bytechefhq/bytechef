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

import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    Optional<String> getDescription();

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
    Optional<ProcessErrorResponseFunction> getProcessErrorResponse();

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
    Optional<Output> getOutput();

    /**
     *
     * @return
     */
    Optional<OutputFunction> getOutputFunction();

    /**
     *
     * @return
     */
    Optional<List<? extends Property>> getProperties();

    /**
     *
     * @return
     */
    Optional<String> getTitle();

    /**
     *
     * @return
     */
    Optional<ActionWorkflowNodeDescriptionFunction> getWorkflowNodeDescription();

    /**
     *
     * @return
     */
    boolean isDefaultOutputFunction();

    /**
     *
     */
    interface PerformFunction {
    }

    /**
     *
     */
    @FunctionalInterface
    interface SingleConnectionPerformFunction extends PerformFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param context
         * @return
         */
        Object apply(Parameters inputParameters, Parameters connectionParameters, ActionContext context)
            throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface ProcessErrorResponseFunction {

        /**
         *
         * @param statusCode
         * @param body
         * @param context
         * @return
         */
        ProviderException apply(int statusCode, Object body, ActionContext context) throws Exception;
    }

    /**
     *
     */
    interface OutputFunction {
    }

    /**
     *
     */
    interface SingleConnectionOutputFunction extends OutputFunction {

        /**
         * @param inputParameters
         * @param connectionParameters
         * @param context
         * @return
         */
        Output apply(
            Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws Exception;
    }
}
