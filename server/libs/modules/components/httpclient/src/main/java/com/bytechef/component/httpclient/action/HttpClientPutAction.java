
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.httpclient.action;

import com.bytechef.component.httpclient.constant.HttpClientConstants;
import com.bytechef.component.httpclient.util.HttpClientActionUtils;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaFunction;

import java.util.Map;

import static com.bytechef.component.httpclient.constant.HttpClientConstants.PUT;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.util.HttpClientUtils.RequestMethod;

/**
 * @author Ivica Cardic
 */
public class HttpClientPutAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(PUT)
        .title("PUT")
        .description("The request method to use.")
        .properties(
            HttpClientActionUtils.toArray(
                //
                // Common properties
                //

                HttpClientConstants.COMMON_PROPERTIES,
                //
                // Body Content properties
                //

                HttpClientConstants.BODY_CONTENT_PROPERTIES,
                //
                // Options
                //

                HttpClientActionUtils.options(true)))
        .outputSchema(getOutputSchemaFunction(), HttpClientConstants.OUTPUT_PROPERTIES)
        .perform((inputParameters, actionContext) -> perform(inputParameters));

    protected static Object perform(Map<String, ?> inputParameters) {
        return HttpClientActionUtils.execute(inputParameters, RequestMethod.PUT);
    }

    protected static OutputSchemaFunction getOutputSchemaFunction() {
        // TODO
        return (connection, inputParameters) -> null;
    }
}
