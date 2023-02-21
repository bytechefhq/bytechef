
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

import java.util.List;

import static com.bytechef.component.httpclient.constant.HttpClientConstants.POST;
import static com.bytechef.component.httpclient.constant.HttpClientConstants.RESPONSE_FORMAT;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.definition.DefinitionDSL.array;
import static com.bytechef.hermes.definition.DefinitionDSL.display;
import static com.bytechef.hermes.definition.DefinitionDSL.object;
import static com.bytechef.hermes.definition.DefinitionDSL.oneOf;
import static com.bytechef.hermes.definition.DefinitionDSL.show;
import static com.bytechef.hermes.definition.DefinitionDSL.string;

/**
 * @author Ivica Cardic
 */
public class HttpClientPostAction {

    public static final ActionDefinition ACTION_DEFINITION = action(POST)
        .display(display("POST").description("The request method to use."))
        .properties(
            HttpClientUtils.toArray(
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

                HttpClientUtils.options(true)))
        .output(
            oneOf().types(array(), object())
                .displayOption(show(
                    RESPONSE_FORMAT,
                    List.of(
                        com.bytechef.hermes.component.utils.HttpClientUtils.ResponseFormat.JSON.name(),
                        com.bytechef.hermes.component.utils.HttpClientUtils.ResponseFormat.XML.name()))),
            string().displayOption(
                show(
                    RESPONSE_FORMAT, com.bytechef.hermes.component.utils.HttpClientUtils.ResponseFormat.TEXT.name())),
            fileEntry().displayOption(
                show(RESPONSE_FORMAT,
                    com.bytechef.hermes.component.utils.HttpClientUtils.ResponseFormat.BINARY.name())))
        .perform(HttpClientPostAction::performPost);

    public static Object performPost(Context context, ExecutionParameters executionParameters) {
        return HttpClientUtils.execute(
            context, executionParameters, com.bytechef.hermes.component.utils.HttpClientUtils.RequestMethod.POST);
    }
}
