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

package com.bytechef.component.google.forms.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.APPLICATION_VND_GOOGLE_APPS_FORM;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM_ID;
import static com.bytechef.component.google.forms.util.GoogleFormsUtils.getCustomResponses;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.google.commons.GoogleUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class GoogleFormsNewResponseTrigger {

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newResponse")
        .title("New Response")
        .description("Triggers when response is submitted to Google Form.")
        .type(TriggerType.POLLING)
        .properties(
            string(FORM_ID)
                .label("Form")
                .description("Form to watch for new responses.")
                .options(GoogleUtils.getFileOptionsByMimeTypeForTriggers(APPLICATION_VND_GOOGLE_APPS_FORM, true))
                .required(true))
        .output()
        .poll(GoogleFormsNewResponseTrigger::poll)
        .processErrorResponse(GoogleUtils::processErrorResponse);

    private GoogleFormsNewResponseTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        Instant now = Instant.now();

        Instant start = closureParameters.get(
            LAST_TIME_CHECKED, Instant.class,
            triggerContext.isEditorEnvironment() ? now.minus(Duration.ofHours(3)) : now);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(start);

        List<Map<String, Object>> customResponses = getCustomResponses(
            triggerContext, inputParameters.getRequiredString(FORM_ID), timestamp);

        return new PollOutput(customResponses, Map.of(LAST_TIME_CHECKED, now), false);
    }
}
