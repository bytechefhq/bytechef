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

package com.bytechef.component.linkedin.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.COMMENT_OUTPUT_PROPERTY;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.COUNT;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.START;
import static com.bytechef.component.linkedin.constant.LinkedInConstants.URN;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.linkedin.util.LinkedInUtils;
import java.util.List;
import java.util.Map;

/**
 * @author Ivica Cardic
 */
public class LinkedInGetCommentsAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getComments")
        .title("Get Comments")
        .description(
            "Get the comments on an organization post, or the replies to a comment when a comment URN is given.")
        .help("", "https://docs.bytechef.io/reference/components/linkedin_v1#get-comments")
        .properties(
            string(URN)
                .label("URN")
                .description(
                    "URN of the post (urn:li:share:{id} or urn:li:ugcPost:{id}) to get the comments of, or URN of " +
                        "the comment (urn:li:comment:({activityUrn},{commentId})) to get the replies to.")
                .required(true),
            integer(START)
                .label("Start")
                .description("Index of the first comment to return.")
                .defaultValue(0)
                .minValue(0)
                .required(false),
            integer(COUNT)
                .label("Count")
                .description("Number of comments to return.")
                .defaultValue(10)
                .minValue(1)
                .maxValue(100)
                .required(false))
        .output(outputSchema(array().items(COMMENT_OUTPUT_PROPERTY)))
        .perform(LinkedInGetCommentsAction::perform);

    private LinkedInGetCommentsAction() {
    }

    public static List<Map<?, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return LinkedInUtils.getComments(
            context, inputParameters.getRequiredString(URN), inputParameters.getInteger(START, 0),
            inputParameters.getInteger(COUNT, 10));
    }
}
