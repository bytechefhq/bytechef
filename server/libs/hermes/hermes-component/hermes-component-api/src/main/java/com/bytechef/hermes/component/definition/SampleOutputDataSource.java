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

/**
 * @author Ivica Cardic
 */
public interface SampleOutputDataSource {

    /**
     * The function that returns an example output.
     *
     * @return The function implementation
     */
    SampleOutputFunction getSampleOutput();

    /**
     *
     */
    @FunctionalInterface
    interface SampleOutputFunction {

        /**
         * @param inputParameters
         * @param connectionParameters
         * @param context
         * @return
         */
        Object apply(ParameterMap inputParameters, ParameterMap connectionParameters, Context context)
            throws ComponentExecutionException;
    }
}
