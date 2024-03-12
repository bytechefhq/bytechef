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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.definition.ComponentDSL.action;
import static com.bytechef.component.definition.ComponentDSL.array;
import static com.bytechef.component.definition.ComponentDSL.bool;
import static com.bytechef.component.definition.ComponentDSL.date;
import static com.bytechef.component.definition.ComponentDSL.dateTime;
import static com.bytechef.component.definition.ComponentDSL.integer;
import static com.bytechef.component.definition.ComponentDSL.nullable;
import static com.bytechef.component.definition.ComponentDSL.number;
import static com.bytechef.component.definition.ComponentDSL.object;
import static com.bytechef.component.definition.ComponentDSL.string;
import static com.bytechef.component.definition.ComponentDSL.time;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.BASE_URL;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CATEGORY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SEARCH_EMAIL;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDSL.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.OptionsDataSource.ActionOptionsFunction;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365Utils;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author Monika Domiter
 */
public class MicrosoftOutlook365SearchEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action(SEARCH_EMAIL)
        .title("Search Email")
        .description("Get the messages in the signed-in user's mailbox")
        .properties(
            string(FROM)
                .label("From")
                .description("The address sending the mail")
                .required(false),
            string(TO)
                .label("To")
                .description("The address receiving the new mail")
                .required(false),
            string(SUBJECT)
                .label("Subject")
                .description("Words in the subject line")
                .required(false),
            string(CATEGORY)
                .label("Category")
                .description("Messages in a certain category")
                .options((ActionOptionsFunction<String>) MicrosoftOutlook365Utils::getCategoryOptions)
                .required(false))
        .outputSchema(
            object()
                .additionalProperties(
                    array(), bool(), date(), dateTime(), integer(), nullable(), number(), object(), string(), time()))
        .perform(MicrosoftOutlook365SearchEmailAction::perform);

    private MicrosoftOutlook365SearchEmailAction() {
    }

    public static Object perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        StringBuilder stringBuilder = new StringBuilder();

        addParameter(stringBuilder, FROM, inputParameters.getString(FROM));
        addParameter(stringBuilder, TO, inputParameters.getString(TO));
        addParameter(stringBuilder, SUBJECT, inputParameters.getString(SUBJECT));
        addParameter(stringBuilder, CATEGORY, inputParameters.getString(CATEGORY));

        String encode = URLEncoder.encode(stringBuilder.toString(), StandardCharsets.UTF_8);

        return context.http(http -> http.get(BASE_URL + "/messages?$search=" + encode))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static void addParameter(StringBuilder stringBuilder, String parameterName, String parameterValue) {
        if (parameterValue != null) {
            if (!parameterName.equals("from") && !stringBuilder.isEmpty()) {
                stringBuilder.append(" AND ");
            }

            stringBuilder.append("\"")
                .append(parameterName)
                .append(":")
                .append(parameterValue)
                .append("\"");
        }
    }
}
