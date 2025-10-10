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

package com.bytechef.component.microsoft.outlook.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.CATEGORY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FROM;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.FULL_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.ODATA_NEXT_LINK;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.SUBJECT;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.TO;
import static com.bytechef.component.microsoft.outlook.constant.MicrosoftOutlook365Constants.VALUE;
import static com.bytechef.microsoft.commons.MicrosoftUtils.getItemsFromNextPage;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.microsoft.outlook.util.MicrosoftOutlook365OptionUtils;
import com.bytechef.microsoft.commons.MicrosoftUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class MicrosoftOutlook365SearchEmailAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("searchEmail")
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
                .options((OptionsFunction<String>) MicrosoftOutlook365OptionUtils::getCategoryOptions)
                .required(false))
        .output(outputSchema(array().items(FULL_MESSAGE_OUTPUT_PROPERTY)))
        .perform(MicrosoftOutlook365SearchEmailAction::perform)
        .processErrorResponse(MicrosoftUtils::processErrorResponse);

    private MicrosoftOutlook365SearchEmailAction() {
    }

    public static List<Map<?, ?>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        StringBuilder stringBuilder = new StringBuilder();

        addParameter(stringBuilder, FROM, inputParameters.getString(FROM));
        addParameter(stringBuilder, TO, inputParameters.getString(TO));
        addParameter(stringBuilder, SUBJECT, inputParameters.getString(SUBJECT));
        addParameter(stringBuilder, CATEGORY, inputParameters.getString(CATEGORY));

        List<Map<?, ?>> emails = new ArrayList<>();

        Map<String, Object> body = context.http(http -> http.get("/me/messages"))
            .queryParameters("$search", stringBuilder.isEmpty() ? null : stringBuilder.toString(), "$top", 100)
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        if (body.get(VALUE) instanceof List<?> list) {
            for (Object o : list) {
                if (o instanceof Map<?, ?> map) {
                    emails.add(map);
                }
            }
        }

        emails.addAll(getItemsFromNextPage((String) body.get(ODATA_NEXT_LINK), context));

        return emails;
    }

    private static void addParameter(StringBuilder stringBuilder, String parameterName, String parameterValue) {
        if (parameterValue != null) {
            if (!parameterName.equals(FROM) && !stringBuilder.isEmpty()) {
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
