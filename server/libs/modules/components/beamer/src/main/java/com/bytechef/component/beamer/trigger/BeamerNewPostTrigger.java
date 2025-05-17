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

package com.bytechef.component.beamer.trigger;

import static com.bytechef.component.beamer.constant.BeamerConstants.CATEGORY;
import static com.bytechef.component.beamer.constant.BeamerConstants.CONTENT;
import static com.bytechef.component.beamer.constant.BeamerConstants.DATE_FROM;
import static com.bytechef.component.beamer.constant.BeamerConstants.LAST_TIME_CHECKED;
import static com.bytechef.component.beamer.constant.BeamerConstants.TITLE;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.definition.Context.Http.responseType;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class BeamerNewPostTrigger {
    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newPost")
        .title("New Post")
        .description("Triggers when a new post is added.")
        .type(TriggerType.POLLING)
        .properties()
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                bool("autoOpen")
                                    .description("Whether the post is auto opened."),
                                string("category")
                                    .description("Category of the post."),
                                integer("clicks")
                                    .description("How many clicks does the post have."),
                                date("date")
                                    .description("Date when the post was created."),
                                bool("feedbackEnabled")
                                    .description("Whether the users can give feedback on the post."),
                                integer("feedbacks")
                                    .description("How many feedbacks does the post have."),
                                integer("negativeReactions")
                                    .description("How many negative reactions does the post have"),
                                integer("neutralReactions")
                                    .description("How many neutral reactions does the post have"),
                                integer("positiveReactions")
                                    .description("How many positive reactions does the post have"),
                                bool("published")
                                    .description("Whether the post is published."),
                                bool("reactionsEnabled")
                                    .description("Whether the reactions are enabled."),
                                array("translations")
                                    .items(
                                        object()
                                            .properties(
                                                string(CATEGORY)
                                                    .description("Custom category of the post."),
                                                string(CONTENT)
                                                    .description("Content of the post (plain text)."),
                                                string("contentHtml")
                                                    .description("Content of the post (original HTML format)."),
                                                string("language")
                                                    .description(
                                                        "Language of th post (in ISO-639 two-letter code format)."),
                                                string("postUrl")
                                                    .description(
                                                        "The URL where users will be redirected when they click on " +
                                                            "the header of the post or the link shown at the bottom " +
                                                            "of it."),
                                                string(TITLE)
                                                    .description("Title of post.")),
                                        integer("uniqueViews")
                                            .description("Number of unique views the post has."),
                                        integer("views")
                                            .description("Number of the views the post has."))))))
        .poll(BeamerNewPostTrigger::poll);

    private BeamerNewPostTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(LAST_TIME_CHECKED, now.minusHours(3));

        List<Map<String, Object>> posts = triggerContext.http(http -> http.get("/posts"))
            .queryParameter(DATE_FROM, startDate.toString())
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return new PollOutput(posts, Map.of(LAST_TIME_CHECKED, now), false);
    }
}
