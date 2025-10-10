/*
 * Copyright 2025 ByteChef
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

package com.bytechef.component.liferay.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.liferay.constant.LiferayConstants.CONTEXT_NAME;
import static com.bytechef.component.liferay.constant.LiferayConstants.ENDPOINT;
import static com.bytechef.component.liferay.constant.LiferayConstants.GET;
import static com.bytechef.component.liferay.constant.LiferayConstants.METHOD;
import static com.bytechef.component.liferay.constant.LiferayConstants.PARAMETERS_DYNAMIC_PROPERTY;
import static com.bytechef.component.liferay.constant.LiferayConstants.POST;
import static com.bytechef.component.liferay.constant.LiferayConstants.SERVICE;
import static com.bytechef.component.liferay.util.LiferayUtils.getContextNameOptions;
import static com.bytechef.component.liferay.util.LiferayUtils.getServiceHttpData;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.liferay.strategy.GetLiferayHttpMethod;
import com.bytechef.component.liferay.strategy.LiferayHttpMethod;
import com.bytechef.component.liferay.strategy.PostLiferayHttpMethod;
import com.bytechef.component.liferay.util.LiferayUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class LiferayJsonWsRequestAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("jsonWsRequest")
        .title("JSON WS Services")
        .description("The Headless endpoint to use.")
        .properties(
            string(CONTEXT_NAME)
                .label("Context Name")
                .description("Context name of JSON web service you want to use.")
                .options(getContextNameOptions())
                .required(true),
            integer(SERVICE)
                .label("Service ID")
                .description("ID of the service you want to access.")
                .optionsLookupDependsOn(CONTEXT_NAME)
                .options((OptionsFunction<Long>) LiferayUtils::getServiceOptions)
                .required(true),
            PARAMETERS_DYNAMIC_PROPERTY)
        .output()
        .perform(LiferayJsonWsRequestAction::perform);

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, String> serviceHttpData = getServiceHttpData(
            context, inputParameters.getRequiredString(CONTEXT_NAME), inputParameters.getRequiredInteger(SERVICE));

        LiferayHttpMethod liferayHttpMethod;

        String method = serviceHttpData.get(METHOD);

        switch (method) {
            case POST ->
                liferayHttpMethod = new PostLiferayHttpMethod();
            case GET ->
                liferayHttpMethod = new GetLiferayHttpMethod();
            default ->
                throw new IllegalArgumentException("Unknown HTTP method: " + method);
        }

        return liferayHttpMethod.perform(inputParameters, connectionParameters, context, serviceHttpData.get(ENDPOINT));
    }
}
