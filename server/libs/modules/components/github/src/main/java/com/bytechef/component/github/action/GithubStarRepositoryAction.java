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

package com.bytechef.component.github.action;

import static com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.github.constant.GithubConstants.OWNER;
import static com.bytechef.component.github.constant.GithubConstants.REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.STAR_REPOSITORY;
import static com.bytechef.component.github.constant.GithubConstants.STAR_REPOSITORY_DESCRIPTION;
import static com.bytechef.component.github.constant.GithubConstants.STAR_REPOSITORY_TITLE;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Monika Kušter
 */
public class GithubStarRepositoryAction {

    @SuppressFBWarnings("MS")
    public static final Property[] PROPERTIES = {
        string(OWNER)
            .label("Owner")
            .description("The account owner of the repository. The name is not case sensitive.")
            .exampleValue("bytechefhq")
            .required(true),
        string(REPOSITORY)
            .label("Repository")
            .description(
                "The name of the repository including owner without the .git extension. The name is not case " +
                    "sensitive.")
            .exampleValue("bytechef")
            .required(true)
    };

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(STAR_REPOSITORY)
        .title(STAR_REPOSITORY_TITLE)
        .description(STAR_REPOSITORY_DESCRIPTION)
        .properties(PROPERTIES)
        .perform(GithubStarRepositoryAction::perform);

    private GithubStarRepositoryAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, Context context) {
        context.http(http -> http.put(
            "/user/starred/" + inputParameters.getRequiredString(OWNER) + "/"
                + inputParameters.getRequiredString(REPOSITORY)))
            .configuration(responseType(ResponseType.JSON))
            .execute();

        return null;
    }
}
