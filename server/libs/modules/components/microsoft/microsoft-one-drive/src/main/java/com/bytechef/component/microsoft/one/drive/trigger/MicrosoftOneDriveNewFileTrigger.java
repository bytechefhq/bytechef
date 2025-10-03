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

package com.bytechef.component.microsoft.one.drive.trigger;

import static com.bytechef.component.definition.ComponentDsl.ModifiableTriggerDefinition;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.ComponentDsl.trigger;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.FILE_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.VALUE;
import static com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils.getFolderId;

import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.OptionsDataSource.TriggerOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TriggerContext;
import com.bytechef.component.definition.TriggerDefinition.PollOutput;
import com.bytechef.component.definition.TriggerDefinition.TriggerType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.one.drive.util.MicrosoftOneDriveUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOneDriveNewFileTrigger {

    public static final ModifiableTriggerDefinition TRIGGER_DEFINITION = trigger("newFile")
        .title("New File")
        .description("Triggers when file is uploaded to folder.")
        .type(TriggerType.POLLING)
        .properties(
            string(PARENT_ID)
                .label("Parent Folder ID")
                .description("If no folder is specified, the root folder will be used.")
                .options((TriggerOptionsFunction<String>) MicrosoftOneDriveUtils::getFolderIdOptions)
                .required(false))
        .output(outputSchema(FILE_OUTPUT_PROPERTY))
        .poll(MicrosoftOneDriveNewFileTrigger::poll)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    protected static final String LAST_TIME_CHECKED = "lastTimeChecked";

    private MicrosoftOneDriveNewFileTrigger() {
    }

    protected static PollOutput poll(
        Parameters inputParameters, Parameters connectionParameters, Parameters closureParameters,
        TriggerContext context) {

        ZoneId zoneId = ZoneId.systemDefault();

        LocalDateTime now = LocalDateTime.now(zoneId);

        LocalDateTime startDate = closureParameters.getLocalDateTime(
            LAST_TIME_CHECKED, context.isEditorEnvironment() ? now.minusHours(3) : now);

        Map<String, Object> body = context
            .http(http -> http
                .get("/me/drive/items/%s/children".formatted(getFolderId(inputParameters.getString(PARENT_ID)))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Map<?, ?>> maps = new ArrayList<>();

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map && map.containsKey("file")) {
                    ZonedDateTime zonedCreatedDateTime = ZonedDateTime.parse((String) map.get("createdDateTime"));

                    LocalDateTime createdDateTime = LocalDateTime.ofInstant(zonedCreatedDateTime.toInstant(), zoneId);

                    if (createdDateTime.isAfter(startDate) && createdDateTime.isBefore(now)) {
                        maps.add(map);
                    }
                }
            }
        }

        return new PollOutput(maps, Map.of(LAST_TIME_CHECKED, now), false);
    }
}
