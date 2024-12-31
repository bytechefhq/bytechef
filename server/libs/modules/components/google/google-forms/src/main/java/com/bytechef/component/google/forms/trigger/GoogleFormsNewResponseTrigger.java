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

package com.bytechef.component.google.forms.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.google.forms.constant.GoogleFormsConstants.FORM;
import static com.bytechef.component.google.forms.util.GoogleFormsUtils.getFormResponses;

import com.bytechef.component.definition.OptionsDataSource.TriggerOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.google.forms.util.GoogleFormsUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
            string(FORM)
                .label("Form")
                .description("Form to watch for new responses.")
                .options((TriggerOptionsFunction<String>) GoogleFormsUtils::getFormOptions)
                .required(true))
        .output(outputSchema(object()))
        .poll(GoogleFormsNewResponseTrigger::poll);

    private GoogleFormsNewResponseTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        ZoneId gmtZoneId = ZoneId.of("GMT");
        LocalDateTime startDate = closureParameters.getLocalDateTime(LAST_TIME_CHECKED, LocalDateTime.now(gmtZoneId));
        String startDateString = startDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
            .withZone(gmtZoneId));
        LocalDateTime endDate = LocalDateTime.now(gmtZoneId);

        List<Map<?, ?>> formResponses = getFormResponses(inputParameters, triggerContext, startDateString);

        return new PollOutput(formResponses, Map.of(LAST_TIME_CHECKED, endDate), false);
    }
}
