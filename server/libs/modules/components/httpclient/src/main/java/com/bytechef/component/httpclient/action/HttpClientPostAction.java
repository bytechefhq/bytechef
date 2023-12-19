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

package com.bytechef.component.httpclient.action;

import static com.bytechef.component.httpclient.constant.HttpClientConstants.POST;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.Context.Http.RequestMethod;

import com.bytechef.component.httpclient.constant.HttpClientConstants;
import com.bytechef.component.httpclient.util.HttpClientActionUtils;
import com.bytechef.hermes.component.definition.ActionContext;
import com.bytechef.hermes.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.ActionOutputSchemaFunction;
import com.bytechef.hermes.component.definition.OutputSchemaDataSource.OutputSchemaResponse;
import com.bytechef.hermes.component.definition.ParameterMap;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.ActionSampleOutputFunction;
import com.bytechef.hermes.component.definition.SampleOutputDataSource.SampleOutputResponse;

/**
 * @author Ivica Cardic
 * @author Igor Beslic
 */
public class HttpClientPostAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(POST)
        .title("POST")
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
        .outputSchema(getOutputSchemaFunction())
        .sampleOutput(getSampleOutputSchemaFunction())
        .perform(HttpClientPostAction::perform);

    protected static ActionOutputSchemaFunction getOutputSchemaFunction() {
        return (inputParameters, connectionParameters, context) -> new OutputSchemaResponse(
            context.outputSchema(outputSchema -> outputSchema.get(
                perform(inputParameters, connectionParameters, context))));
    }

    protected static ActionSampleOutputFunction getSampleOutputSchemaFunction() {
        return (inputParameters, connectionParameters, context) -> new SampleOutputResponse(
            perform(inputParameters, connectionParameters, context));
    }

    protected static Object perform(
        ParameterMap inputParameters, ParameterMap connectionParameters, ActionContext context) {

        return HttpClientActionUtils.execute(inputParameters, RequestMethod.POST, context);
    }
}
