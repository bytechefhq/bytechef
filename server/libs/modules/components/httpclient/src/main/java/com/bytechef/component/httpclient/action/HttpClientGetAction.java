
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
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.InputParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;

import static com.bytechef.component.httpclient.constant.HttpClientConstants.GET;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.RESPONSE_FORMAT;
import static com.bytechef.hermes.component.util.HttpClientUtils.RequestMethod;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;

import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class HttpClientGetAction {

    public static final ActionDefinition ACTION_DEFINITION = action(GET)
        .title("GET")
        .description("The request method to use.")
        .properties(
            HttpClientActionUtils.toArray(
                //
                // Common properties
                //

                HttpClientConstants.COMMON_PROPERTIES))
        .outputSchema(
            oneOf()
                .types(array(), object())
                .displayCondition(
                    "['%s', '%s'].includes('%s')".formatted(
                        ResponseFormat.JSON.name(),
                        ResponseFormat.XML.name(),
                        RESPONSE_FORMAT)),
            string().displayCondition("%s === '%s'".formatted(RESPONSE_FORMAT, ResponseFormat.TEXT.name())),
            fileEntry().displayCondition("%s === '%s'".formatted(RESPONSE_FORMAT, ResponseFormat.BINARY.name())))
        .execute(HttpClientGetAction::executeGet);

    public static Object executeGet(Context context, InputParameters inputParameters) {
        return HttpClientActionUtils.execute(context, inputParameters, RequestMethod.GET);
    }
}
