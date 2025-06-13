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

package com.bytechef.component.contiguity.action;

import static com.bytechef.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.BodyContentType;
import static com.bytechef.component.definition.Context.Http.ResponseType;

import com.bytechef.component.definition.ComponentDsl;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class ContiguitySendEmailAction {
    public static final ComponentDsl.ModifiableActionDefinition ACTION_DEFINITION = action("sendEmail")
        .title("Send Email")
        .description("Send email.")
        .metadata(
            Map.of(
                "method", "POST",
                "path", "/send/email", "bodyContentType", BodyContentType.JSON, "mimeType", "application/json"

            ))
        .properties(string("to").metadata(
            Map.of(
                "type", PropertyType.BODY))
            .label("To")
            .description("Recipient's email address.")
            .required(true),
            string("from").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("From")
                .description("Sender's name.")
                .required(true),
            string("subject").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Subject")
                .description("Email subject.")
                .required(true),
            string("body").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Body")
                .description("Email content.")
                .required(true),
            string("contentType").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Content Type")
                .description("Content type of the email.")
                .options(option("Html", "html"), option("Text", "text"))
                .required(true),
            string("cc").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("CC")
                .description("CC email address (only 1 is supported as of now).")
                .required(false),
            string("replyTo").metadata(
                Map.of(
                    "type", PropertyType.BODY))
                .label("Reply To")
                .description("Reply-to email address.")
                .required(false))
        .output(outputSchema(object().properties(integer("code").description("Status code of response.")
            .required(false),
            string("message").description("Response message.")
                .required(false),
            object("crumbs").properties(string("plan").description("Subscription plan of the sender.")
                .required(false),
                integer("quota").description("How many messages of same type were already sent.")
                    .required(false),
                string("type").description("Type of message that was sent; SMS or email.")
                    .required(false),
                bool("ad").description("Whether the message was an ad or not.")
                    .required(false))
                .description("Crumbs of the message that was sent.")
                .required(false))
            .metadata(
                Map.of(
                    "responseType", ResponseType.JSON))));

    private ContiguitySendEmailAction() {
    }
}
