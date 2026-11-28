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

package com.bytechef.ai.copilot.tool.context;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class LookupPropertyTrackerTest {

    private static final String SLACK_PROPERTIES = """
        {
            "channel": { "value": "string (required)", "lookupRequired": true, "lookupDependsOn": [] },
            "text": "string (required)"
        }""";

    @Test
    void testFindsAnUnresolvedLookupPropertyTheQuestionAsksFor() {
        ToolContext toolContext = newToolContext();

        LookupPropertyTracker.record(toolContext, "getTaskProperties", "{}", SLACK_PROPERTIES);

        assertThat(LookupPropertyTracker.fetchUnresolvedPropertyMentionedIn(
            toolContext, List.of("Channel", "Which Slack channel should the reminder be posted to?")))
                .isEqualTo("channel");
    }

    @Test
    void testIgnoresAPropertyThePickerAlreadyResolved() {
        ToolContext toolContext = newToolContext();

        LookupPropertyTracker.record(toolContext, "getTaskProperties", "{}", SLACK_PROPERTIES);
        LookupPropertyTracker.record(
            toolContext, "selectPropertyOption",
            "{\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\",\"propertyName\":\"channel\"}",
            "{\"kind\":\"select-property-option\"}");

        assertThat(LookupPropertyTracker.fetchUnresolvedPropertyMentionedIn(
            toolContext, List.of("Which Slack channel?"))).isNull();
    }

    @Test
    void testKeepsAPropertyUnresolvedAfterOnlyALookup() {
        ToolContext toolContext = newToolContext();

        LookupPropertyTracker.record(toolContext, "getTaskProperties", "{}", SLACK_PROPERTIES);
        LookupPropertyTracker.record(
            toolContext, "lookupActionPropertyOptions",
            "{\"componentName\":\"slack\",\"actionName\":\"sendChannelMessage\",\"propertyName\":\"channel\"}",
            "{\"options\":[{\"label\":\"general\",\"value\":\"C06H2PR8LSV\"}]}");

        assertThat(LookupPropertyTracker.fetchUnresolvedPropertyMentionedIn(
            toolContext, List.of("Which Slack channel?"))).isEqualTo("channel");
    }

    @Test
    void testIgnoresQuestionsAboutOtherThings() {
        ToolContext toolContext = newToolContext();

        LookupPropertyTracker.record(toolContext, "getTaskProperties", "{}", SLACK_PROPERTIES);

        assertThat(LookupPropertyTracker.fetchUnresolvedPropertyMentionedIn(
            toolContext, List.of("Timezone", "What timezone should the 9am schedule run in?"))).isNull();
        assertThat(LookupPropertyTracker.fetchUnresolvedPropertyMentionedIn(
            toolContext, List.of("Which text should the message contain?"))).isNull();
    }

    @Test
    void testMatchesTheSignificantWordsOfACamelCasePropertyName() {
        ToolContext toolContext = newToolContext();

        LookupPropertyTracker.record(
            toolContext, "getTaskProperties", "{}",
            "{\"spreadsheetId\": { \"value\": \"string\", \"lookupRequired\": true, \"lookupDependsOn\": [] }}");

        assertThat(LookupPropertyTracker.fetchUnresolvedPropertyMentionedIn(
            toolContext, List.of("Which spreadsheet should the rows go to?"))).isEqualTo("spreadsheetId");
        assertThat(LookupPropertyTracker.fetchUnresolvedPropertyMentionedIn(
            toolContext, List.of("Which ID format do you want?"))).isNull();
    }

    @Test
    void testDoesNothingWithoutATracker() {
        ToolContext toolContext = new ToolContext(Map.of());

        LookupPropertyTracker.record(toolContext, "getTaskProperties", "{}", SLACK_PROPERTIES);

        assertThat(LookupPropertyTracker.fetchUnresolvedPropertyMentionedIn(
            toolContext, List.of("Which Slack channel?"))).isNull();
    }

    private static ToolContext newToolContext() {
        Map<String, Object> context = new HashMap<>();

        LookupPropertyTracker.install(context);

        return new ToolContext(context);
    }
}
