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

package com.bytechef.component.twilio.constant;

import static com.bytechef.component.definition.ComponentDsl.dateTime;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl;

/**
 * @author Monika Kušter
 */
public class TwilioConstants {

    private TwilioConstants() {
    }

    public static final String CONTENT_SID = "ContentSid";
    public static final String CONTENT_VARIABLES = "ContentVariables";
    public static final String BODY = "Body";
    public static final String DATE_TIME = "dateTime";
    public static final String FROM = "From";
    public static final String TO = "To";
    public static final String USE_TEMPLATE = "useTemplate";
    public static final String ZONE_ID = "zoneId";

    public static final ComponentDsl.ModifiableObjectProperty MESSAGE_OUTPUT_PROPERTY = object()
        .properties(
            string("body"),
            string("numSegments"),
            string("direction"),
            object("from")
                .properties(
                    string("rawNumber")),
            string("to"),
            object("dateUpdated")
                .properties(
                    dateTime(DATE_TIME),
                    string(ZONE_ID)),
            string("price"),
            string("errorMessage"),
            string("uri"),
            string("accountSid"),
            string("numMedia"),
            string("status"),
            string("messagingServiceSid"),
            string("sid"),
            object("dateSent")
                .properties(
                    dateTime(DATE_TIME),
                    string(ZONE_ID)),
            object("dateCreated")
                .properties(
                    dateTime(DATE_TIME),
                    string(ZONE_ID)),
            integer("errorCode"),
            object("currency")
                .properties(
                    string("currencyCode"),
                    integer("defaultFractionDigits"),
                    integer("numericCode")),
            string("apiVersion"),
            object("subresourceUris")
                .additionalProperties(string()));
}
