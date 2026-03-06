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

package com.bytechef.component.microsoft.share.point.trigger;

import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.PARENT_FOLDER;
import static com.bytechef.component.microsoft.share.point.constant.MicrosoftSharePointConstants.SITE_ID;

import com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.OptionsFunction;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.microsoft.share.point.util.MicrosoftSharePointUtils;
import com.bytechef.microsoft.commons.MicrosoftTriggerUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;

/**
 * @author Nikolina Spehar
 */
public class MicrosoftSharePointNewFileTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newFile")
        .title("New File")
        .description("Triggers when file is uploaded to folder.")
        .type(TriggerType.POLLING)
        .help("", "https://docs.bytechef.io/reference/components/microsoft-share-point_v1#new-file")
        .properties(
            string(SITE_ID)
                .label("Site ID")
                .description("The ID of the SharePoint site.")
                .options((OptionsFunction<String>) MicrosoftSharePointUtils::getSiteOptions)
                .required(true),
            string(PARENT_FOLDER)
                .label("Parent Folder ID")
                .description("If no folder is selected, root folder will be monitored for new file.")
                .optionsLookupDependsOn(SITE_ID)
                .options((OptionsFunction<String>) MicrosoftSharePointUtils::getFolderIdOptions)
                .required(false))
        .output()
        .poll(MicrosoftSharePointNewFileTrigger::poll)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftSharePointNewFileTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        String url = "/sites/%s/drive/items/%s/children".formatted(
            inputParameters.getRequiredString(SITE_ID), inputParameters.getString(PARENT_FOLDER, "root"));

        return MicrosoftTriggerUtils.poll(url, "file", closureParameters, context);
    }
}
