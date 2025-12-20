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

package com.bytechef.component.rss.trigger;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.rss.constant.RssConstants.ITEM_OBJECT;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.HttpHeaders;
import com.bytechef.component.definition.TriggerDefinition.HttpParameters;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TriggerDefinition.WebhookBody;
import com.bytechef.component.definition.TriggerDefinition.WebhookMethod;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class RssNewItemInFeedTrigger {
    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newItemInFeed")
        .title("New Item in Feed")
        .description("Triggers when a new item is added to the feed.")
        .type(TriggerType.STATIC_WEBHOOK)
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("Event ID."),
                        string("type")
                            .description("Type of the event."),
                        object("feed")
                            .description("Feed object.")
                            .properties(
                                string("id")
                                    .description("Feed ID."),
                                string("title")
                                    .description("Feed title."),
                                string("source_url")
                                    .description("Feed source URL."),
                                string("rss_feed_url")
                                    .description("Feed RSS feed URL."),
                                string("description")
                                    .description("Description of the feed."),
                                string("icon")
                                    .description("Icon of the feed.")),
                        object("data")
                            .description("Feed data.")
                            .properties(
                                array("items_new")
                                    .description("New items on the feed.")
                                    .items(ITEM_OBJECT),
                                array("items_changed")
                                    .description("Changed items on the feed.")
                                    .items(ITEM_OBJECT)))))
        .webhookRequest(RssNewItemInFeedTrigger::webhookRequest);

    private RssNewItemInFeedTrigger() {
    }

    protected static Map<String, Object> webhookRequest(
        Parameters inputParameters, Parameters connectionParameters, HttpHeaders headers, HttpParameters parameters,
        WebhookBody body, WebhookMethod method, Parameters webhookEnableOutput, TriggerContext context) {

        return body.getContent(new TypeReference<>() {});
    }
}
