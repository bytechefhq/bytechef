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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public interface OptionsDataSource {

    /**
     *
     * @return
     */
    OptionsFunction getOptions();

    /**
     *
     * @return
     */
    default Optional<List<String>> getOptionsLookupDependsOn() {
        return Optional.empty();
    }

    /**
     *
     */
    interface OptionsFunction {
    }

    /**
     *
     */
    @FunctionalInterface
    interface ActionOptionsFunction<T> extends OptionsFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param lookupDependsOnPaths
         * @param searchText
         * @param context
         * @return
         * @throws Exception
         */
        List<? extends Option<T>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
            String searchText, ActionContext context) throws Exception;
    }

    /**
     *
     */
    @FunctionalInterface
    interface TriggerOptionsFunction<T> extends OptionsFunction {

        /**
         *
         * @param inputParameters
         * @param connectionParameters
         * @param lookupDependsOnPaths
         * @param searchText
         * @param context
         * @return
         * @throws Exception
         */
        List<? extends Option<T>> apply(
            Parameters inputParameters, Parameters connectionParameters, Map<String, String> lookupDependsOnPaths,
            String searchText, TriggerContext context) throws Exception;
    }
}
