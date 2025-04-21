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

package com.bytechef.component.mautic.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.date;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.mautic.constant.MauticConstants.EMAIL;
import static com.bytechef.component.mautic.constant.MauticConstants.FIRSTNAME;
import static com.bytechef.component.mautic.constant.MauticConstants.IS_PUBLISHED;
import static com.bytechef.component.mautic.constant.MauticConstants.LASTNAME;
import static com.bytechef.component.mautic.constant.MauticConstants.OVERWRITE_WITH_BLANK;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class MauticCreateContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createContact")
        .title("Create Contact")
        .description("Creates a new contact.")
        .properties(
            string(FIRSTNAME)
                .label("First Name")
                .required(true),
            string(LASTNAME)
                .label("Last Name")
                .required(true),
            string(EMAIL)
                .label("Email")
                .required(true),
            bool(OVERWRITE_WITH_BLANK)
                .label("Overwrite With Blank")
                .description(
                    "If true, then empty values are set to fields." +
                        "Otherwise empty values are skipped.")
                .defaultValue(false)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        string("id")
                            .description("ID of the contact."),
                        bool(IS_PUBLISHED)
                            .description("Whether the contact is published."),
                        date("dateAdded")
                            .description("Date/time contact was created."),
                        integer("createdBy")
                            .description("ID of the user that created the contact."),
                        string("createdByUser")
                            .description("Name of the user that created the contact."),
                        date("dateModified")
                            .description("Date/time company was last modified."),
                        integer("modifiedBy")
                            .description("ID of the user that last modified the contact."),
                        string("modifiedByUser")
                            .description("Name of the user that last modified the contact."),
                        object("owner")
                            .description("User object that owns the contact."),
                        integer("points")
                            .description("Contact's current number of points."),
                        date("lastActive")
                            .description("Date/time for when the contact was last recorded as active."),
                        date("dateIdentified")
                            .description("Date/time when the contact identified themselves."),
                        string("color")
                            .description(
                                "Hex value given to contact from Point Trigger definitions based" +
                                    " on the number of points the contact has been awarded."),
                        array("ipAddresses")
                            .description("Array of IPs currently associated with this contact."),
                        array("fields")
                            .description("Custom fields for the contact."),
                        array("tags")
                            .description("Array of tags associated with this contact."),
                        array("utmtags")
                            .description("Array of UTM Tags associated with this contact."),
                        array("doNotContact")
                            .description("Array of Do Not Contact objects."))))
        .perform(MauticCreateContactAction::perform);

    private MauticCreateContactAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.post("/contacts/new"))
            .body(
                Body.of(
                    FIRSTNAME, inputParameters.getRequiredString(FIRSTNAME),
                    LASTNAME, inputParameters.getRequiredString(LASTNAME),
                    EMAIL, inputParameters.getRequiredString(EMAIL),
                    OVERWRITE_WITH_BLANK, inputParameters.getBoolean(OVERWRITE_WITH_BLANK)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
