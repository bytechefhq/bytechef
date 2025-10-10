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

package com.bytechef.component.salesforce.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.salesforce.constant.SalesforceConstants.OBJECT;
import static com.bytechef.component.salesforce.util.SalesforceUtils.getPollOutput;

import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.salesforce.util.SalesforceUtils;

/**
 * @author Monika Kušter
 */
public class SalesforceNewRecordTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newRecord")
        .title("New Record")
        .description("Triggers when there is new record in Salesforce.")
        .type(TriggerType.POLLING)
        .properties(
            string(OBJECT)
                .label("Salesforce Object")
                .options((OptionsFunction<String>) SalesforceUtils::getSalesforceObjectOptions)
                .required(true))
        .output()
        .poll(SalesforceNewRecordTrigger::poll);

    private SalesforceNewRecordTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext triggerContext) {

        return getPollOutput(inputParameters, closureParameters, triggerContext, "CreatedDate");
    }
}
