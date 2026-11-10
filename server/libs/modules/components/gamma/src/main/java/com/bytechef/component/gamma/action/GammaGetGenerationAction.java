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

package com.bytechef.component.gamma.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.gamma.constant.GammaConstants.GENERATION_ID;
import static com.bytechef.component.gamma.constant.GammaConstants.GENERATION_OUTPUT_PROPERTY;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class GammaGetGenerationAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getGeneration")
        .title("Get Generation")
        .description(
            "Get the status of a generation. When the status is completed, the result includes the gamma URL and, " +
                "if an export was requested, the export URL.")
        .properties(
            string(GENERATION_ID)
                .label("Generation ID")
                .description("The ID of the generation returned by Generate Presentation.")
                .required(true))
        .output(outputSchema(GENERATION_OUTPUT_PROPERTY))
        .help("", "https://docs.bytechef.io/reference/components/gamma_v1#get-generation")
        .perform(GammaGetGenerationAction::perform);

    private GammaGetGenerationAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        return context.http(http -> http.get("/generations/" + inputParameters.getRequiredString(GENERATION_ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
