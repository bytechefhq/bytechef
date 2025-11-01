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

package com.bytechef.component.infobip.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.dynamicProperties;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.infobip.constant.InfobipConstants.CONTENT;
import static com.bytechef.component.infobip.constant.InfobipConstants.FROM;
import static com.bytechef.component.infobip.constant.InfobipConstants.LANGUAGE;
import static com.bytechef.component.infobip.constant.InfobipConstants.NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.PLACEHOLDERS;
import static com.bytechef.component.infobip.constant.InfobipConstants.TEMPLATE_NAME;
import static com.bytechef.component.infobip.constant.InfobipConstants.TO;
import static com.bytechef.component.infobip.constant.InfobipConstants.WHATSAPP_MESSAGE_OUTPUT_PROPERTY;
import static com.bytechef.component.infobip.util.InfobipUtils.getTemplates;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.infobip.util.InfobipUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Kušter
 */
public class InfobipSendWhatsAppTemplateMessageAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("sendWhatsappTemplateMessage")
        .title("Send Whatsapp Template Message")
        .description("Send a template message.")
        .properties(
            string(FROM)
                .label("From")
                .description(
                    "Registered WhatsApp sender number. Must be in international format and comply with " +
                        "WhatsApp's requirements.")
                .maxLength(24)
                .required(true),
            string(TO)
                .label("To")
                .description("Message recipient number. Must be in international format.")
                .maxLength(24)
                .required(true),
            string(TEMPLATE_NAME)
                .label("Template Name")
                .description("Name of the WhatsApp template to use.")
                .optionsLookupDependsOn(FROM)
                .options((OptionsFunction<String>) InfobipUtils::getTemplateOptions)
                .required(true),
            dynamicProperties(PLACEHOLDERS)
                .properties(InfobipUtils::createPlaceholderProperties)
                .propertiesLookupDependsOn(TEMPLATE_NAME))
        .output(
            outputSchema(
                object()
                    .properties(
                        array("messages")
                            .items(WHATSAPP_MESSAGE_OUTPUT_PROPERTY))))
        .perform(InfobipSendWhatsAppTemplateMessageAction::perform);

    private InfobipSendWhatsAppTemplateMessageAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        String from = inputParameters.getRequiredString(FROM);
        String templateName = inputParameters.getRequiredString(TEMPLATE_NAME);
        String language = findTemplateLanguage(from, templateName, context);

        List<String> placeholders = extractPlaceholders(inputParameters);

        return context
            .http(http -> http.post("/whatsapp/1/message/template"))
            .body(
                Http.Body.of(
                    "messages", List.of(
                        Map.of(
                            FROM, from,
                            TO, inputParameters.getRequiredString(TO),
                            CONTENT, Map.of(
                                TEMPLATE_NAME, templateName,
                                "templateData", Map.of(
                                    "body", Map.of(
                                        PLACEHOLDERS, placeholders)),
                                LANGUAGE, language)))))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }

    private static String findTemplateLanguage(String from, String templateName, Context context) {
        for (Map<String, Object> map : getTemplates(from, context)) {
            Object name = map.get(NAME);

            if (name.equals(templateName)) {
                return (String) map.get(LANGUAGE);
            }
        }

        return "";
    }

    private static List<String> extractPlaceholders(Parameters inputParameters) {
        List<String> placeholders = new ArrayList<>();

        inputParameters.getMap(PLACEHOLDERS, String.class, Map.of())
            .forEach((key, value) -> placeholders.add(value));

        return placeholders;
    }
}
