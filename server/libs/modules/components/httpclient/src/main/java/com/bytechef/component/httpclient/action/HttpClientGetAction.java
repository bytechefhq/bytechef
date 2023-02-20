
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
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ActionDefinition;
import com.bytechef.hermes.component.utils.HttpClientUtils;

import java.util.List;

import static com.bytechef.component.httpclient.constant.HttpClientConstants.GET;
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
public class HttpClientGetAction {

    public static final ActionDefinition GET_ACTION = action(GET)
        .display(display("GET").description("The request method to use."))
        .properties(
            com.bytechef.component.httpclient.util.HttpClientUtils.toArray(
                //
                // Common properties
                //

                HttpClientConstants.COMMON_PROPERTIES))
        .output(
            oneOf()
                .types(array(), object())
                .displayOption(
                    show(
                        RESPONSE_FORMAT,
                        List.of(HttpClientUtils.ResponseFormat.JSON.name(),
                            HttpClientUtils.ResponseFormat.XML.name()))),
            string().displayOption(show(RESPONSE_FORMAT, HttpClientUtils.ResponseFormat.TEXT.name())),
            fileEntry().displayOption(show(RESPONSE_FORMAT, HttpClientUtils.ResponseFormat.BINARY.name())))
        .perform(HttpClientGetAction::performGet);

    public static Object performGet(Context context, ExecutionParameters executionParameters) {
        return com.bytechef.component.httpclient.util.HttpClientUtils.execute(context, executionParameters,
            HttpClientUtils.RequestMethod.GET);
    }
}
