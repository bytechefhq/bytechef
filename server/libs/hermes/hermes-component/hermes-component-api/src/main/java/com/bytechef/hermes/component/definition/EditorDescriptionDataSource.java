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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
public interface EditorDescriptionDataSource {

    EditorDescriptionFunction getEditorDescription();

    interface EditorDescriptionFunction {

        /**
         * @param inputParameters
         * @param context
         * @return
         */
        EditorDescriptionResponse apply(ParameterMap inputParameters, Context context)
            throws ComponentExecutionException;
    }

    @SuppressFBWarnings("EI")
    record EditorDescriptionResponse(String description, String errorMessage) {

        public EditorDescriptionResponse(String description) {
            this(description, null);
        }
    }
}
