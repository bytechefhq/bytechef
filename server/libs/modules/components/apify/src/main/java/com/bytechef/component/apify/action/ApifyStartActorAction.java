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

package com.bytechef.component.apify.action;

import static com.bytechef.component.apify.constant.ApifyConstants.ACTOR_ID;
import static com.bytechef.component.apify.constant.ApifyConstants.BODY;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.apify.util.ApifyUtils;
import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ApifyStartActorAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("startActor")
        .title("Start Actor")
        .description("Starts an Apify Actor")
        .properties(
            string(ACTOR_ID)
                .label("Actor ID")
                .description("ID of the actor that will be run.")
                .required(true)
                .options((OptionsFunction<String>) ApifyUtils::getActorIdOptions),
            string(BODY)
                .label("Body")
                .description(
                    "The JSON input to pass to the Actor [you can get the JSON from a run in your Apify account].")
                .controlType(ControlType.TEXT_AREA)
                .required(true))
        .output()
        .perform(ApifyStartActorAction::perform);

    private ApifyStartActorAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        Map<String, Object> jsonBody = context.json(
            json -> json.read(inputParameters.getRequiredString(BODY), new TypeReference<>() {}));

        return context.http(
            http -> http.post("/acts/%s/runs".formatted(inputParameters.getRequiredString(ACTOR_ID))))
            .body(Body.of(jsonBody))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody();
    }
}
