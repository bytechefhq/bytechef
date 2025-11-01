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
import static com.bytechef.component.mautic.constant.MauticConstants.ID;
import static com.bytechef.component.mautic.constant.MauticConstants.IS_PUBLISHED;

import com.bytechef.component.definition.ActionDefinition.OptionsFunction;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.mautic.util.MauticUtils;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class MauticGetContactAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("getContact")
        .title("Get Contact")
        .description("Get individual contact.")
        .properties(
            string(ID)
                .label("Contact ID")
                .description("ID of the contact you want to retrieve.")
                .options((OptionsFunction<String>) MauticUtils::getContactOptions)
                .required(true))
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
        .perform(MauticGetContactAction::perform);

    private MauticGetContactAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        return context.http(http -> http.get("/contacts/" + inputParameters.getRequiredString(ID)))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});
    }
}
