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

package com.bytechef.component.jenkins.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.jenkins.constant.JenkinsConstants.CONFIG_XML;
import static com.bytechef.component.jenkins.constant.JenkinsConstants.NAME;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;

/**
 * @author Nikolina Spehar
 */
public class JenkinsCreateJobAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createJob")
        .title("Create Job")
        .description("Creates a new job.")
        .properties(
            string(NAME)
                .label("Name")
                .description("Name of the job.")
                .required(true),
            string(CONFIG_XML)
                .label("Config XML")
                .description("Content of the config.xml file.")
                .required(true))
        .perform(JenkinsCreateJobAction::perform);

    private JenkinsCreateJobAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.post("/createItem"))
            .header("Content-Type", "application/xml")
            .queryParameter(NAME, inputParameters.getRequiredString(NAME))
            .body(Body.of(inputParameters.getRequiredString(CONFIG_XML), "application/xml"))
            .execute();

        return null;
    }
}
