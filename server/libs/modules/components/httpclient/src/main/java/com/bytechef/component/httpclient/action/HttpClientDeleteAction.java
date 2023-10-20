
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
import com.bytechef.component.httpclient.util.HttpClientUtils;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;

import static com.bytechef.component.httpclient.constant.HttpClientConstants.DELETE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.definition.DefinitionDSL.display;

/**
 * @author Ivica Cardic
 */
public class HttpClientDeleteAction {

    public static final ActionDefinition DELETE_ACTION = action(DELETE)
        .display(display("DELETE").description("The request method to use."))
        .properties(
            HttpClientUtils.toArray(
                //
                // Common properties
                //

                HttpClientConstants.COMMON_PROPERTIES,
                //
                // Options
                //

                HttpClientUtils.options(false)))
        .output(HttpClientUtils.toArray(HttpClientConstants.OUTPUT_PROPERTIES))
        .perform(HttpClientDeleteAction::performDelete);

    public static Object performDelete(Context context, ExecutionParameters executionParameters) {
        return HttpClientUtils.execute(context, executionParameters,
            com.bytechef.hermes.component.utils.HttpClientUtils.RequestMethod.DELETE);
    }
}
