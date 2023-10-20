
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
import com.bytechef.hermes.component.definition.ActionDefinition;

import java.util.Map;

import static com.bytechef.component.httpclient.constant.HttpClientConstants.POST;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.RESPONSE_FORMAT;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.util.HttpClientUtils.RequestMethod;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;
import static com.bytechef.hermes.definition.DefinitionDSL.array;

import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class HttpClientPostAction {

    public static final ActionDefinition ACTION_DEFINITION = action(POST)
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
        .outputSchema(
            oneOf().types(array(), object())
                .displayCondition("['%s', '%s'].includes('%s')".formatted(
                    ResponseFormat.JSON.name(),
                    ResponseFormat.XML.name(),
                    RESPONSE_FORMAT)),
            string().displayCondition("%s === '%s'".formatted(RESPONSE_FORMAT, ResponseFormat.TEXT.name())),
            fileEntry().displayCondition("%s === '%s'".formatted(RESPONSE_FORMAT, ResponseFormat.BINARY.name())))
        .execute((actionContext, inputParameters) -> executePost(inputParameters));

    protected static Object executePost(Map<String, ?> inputParameters) {
        return HttpClientActionUtils.execute(inputParameters, RequestMethod.POST);
    }
}
