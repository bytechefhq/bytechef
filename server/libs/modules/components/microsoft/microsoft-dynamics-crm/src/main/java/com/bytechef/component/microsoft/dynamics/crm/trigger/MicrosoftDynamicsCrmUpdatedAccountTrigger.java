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

package com.bytechef.component.microsoft.dynamics.crm.trigger;

import static com.bytechef.component.definition.ComponentDsl.trigger;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.microsoft.dynamics.crm.util.MicrosoftDynamicsCrmUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Monika Kušter
 */
public class MicrosoftDynamicsCrmUpdatedAccountTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("updatedAccount")
        .title("Updated Account")
        .description("Triggers when an account is updated.")
        .type(TriggerType.POLLING)
        .help("", "https://docs.bytechef.io/reference/components/microsoft-dynamics-crm_v1#updated-account")
        .output()
        .poll(MicrosoftDynamicsCrmUpdatedAccountTrigger::poll)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftDynamicsCrmUpdatedAccountTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        return MicrosoftDynamicsCrmUtils.poll(closureParameters, context, "modifiedon");
    }
}
