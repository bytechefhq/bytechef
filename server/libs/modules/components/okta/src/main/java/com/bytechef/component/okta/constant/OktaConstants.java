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

package com.bytechef.component.okta.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableIntegerProperty;
import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Ivica Cardic
 */
public class OktaConstants {

    public static final String APP_ID = "appId";
    public static final String DOMAIN = "domain";
    public static final String FILTER = "filter";
    public static final String GROUP_ID = "groupId";
    public static final String ID = "id";
    public static final String LABEL = "label";
    public static final String LIMIT = "limit";
    public static final String NAME = "name";
    public static final String PROFILE = "profile";
    public static final String Q = "q";
    public static final String SEARCH = "search";
    public static final String USER_ID = "userId";

    public static final ModifiableIntegerProperty LIMIT_PROPERTY = integer(LIMIT)
        .label("Limit")
        .description(
            "Maximum number of results to return. Results are fetched page by page until this number is reached. " +
                "Leave empty to return all results.")
        .minValue(1)
        .required(false);

    public static final ModifiableObjectProperty USER_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The unique ID of the user."),
            string("status")
                .description(
                    "The status of the user, for example ACTIVE, STAGED, PROVISIONED, SUSPENDED, LOCKED_OUT, " +
                        "PASSWORD_EXPIRED, RECOVERY or DEPROVISIONED."),
            string("created")
                .description("ISO 8601 timestamp for when the user was created."),
            string("activated")
                .description("ISO 8601 timestamp for when the user was activated."),
            string("statusChanged")
                .description("ISO 8601 timestamp for when the status of the user last changed."),
            string("lastLogin")
                .description("ISO 8601 timestamp of the last login of the user."),
            string("lastUpdated")
                .description("ISO 8601 timestamp for when the user was last updated."),
            string("passwordChanged")
                .description("ISO 8601 timestamp for when the password of the user last changed."),
            object("type")
                .description("The user type.")
                .properties(
                    string(ID)
                        .description("The ID of the user type.")),
            object(PROFILE)
                .description("The profile properties of the user.")
                .properties(
                    string("login")
                        .description("The unique login of the user."),
                    string("email")
                        .description("The primary email address of the user."),
                    string("secondEmail")
                        .description("The secondary email address of the user."),
                    string("firstName")
                        .description("The first name of the user."),
                    string("lastName")
                        .description("The last name of the user."),
                    string("displayName")
                        .description("The display name of the user."),
                    string("mobilePhone")
                        .description("The mobile phone number of the user."),
                    string("title")
                        .description("The title of the user."),
                    string("department")
                        .description("The department of the user.")));

    public static final ModifiableObjectProperty GROUP_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The unique ID of the group."),
            string("type")
                .description("The type of the group: OKTA_GROUP, APP_GROUP or BUILT_IN."),
            string("created")
                .description("ISO 8601 timestamp for when the group was created."),
            string("lastUpdated")
                .description("ISO 8601 timestamp for when the group profile was last updated."),
            string("lastMembershipUpdated")
                .description("ISO 8601 timestamp for when the group memberships were last updated."),
            array("objectClass")
                .description("The object classes of the group.")
                .items(string()),
            object(PROFILE)
                .description("The profile properties of the group.")
                .properties(
                    string(NAME)
                        .description("The name of the group."),
                    string("description")
                        .description("The description of the group.")));

    public static final ModifiableObjectProperty APPLICATION_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The unique ID of the app instance."),
            string(NAME)
                .description("The key name of the app."),
            string(LABEL)
                .description("The user-defined display name of the app."),
            string("status")
                .description("The status of the app: ACTIVE or INACTIVE."),
            string("signOnMode")
                .description("The authentication mode of the app."),
            string("created")
                .description("ISO 8601 timestamp for when the app was created."),
            string("lastUpdated")
                .description("ISO 8601 timestamp for when the app was last updated."));

    public static final ModifiableObjectProperty APP_LINK_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The ID of the app link."),
            string(LABEL)
                .description("The display name of the app link."),
            string("linkUrl")
                .description("The URL of the app link."),
            string("logoUrl")
                .description("The URL of the app logo."),
            string("appName")
                .description("The name of the app."),
            string("appInstanceId")
                .description("The ID of the app instance."),
            string("appAssignmentId")
                .description("The ID of the app assignment."),
            bool("credentialsSetup")
                .description("Whether credentials setup is required."),
            bool("hidden")
                .description("Whether the app link is hidden."),
            integer("sortOrder")
                .description("The position of the app link in the sort order."));

    public static final ModifiableObjectProperty APPLICATION_USER_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The Okta ID of the user."),
            string("externalId")
                .description("The ID of the user in the target app."),
            string("scope")
                .description("USER for a direct assignment or GROUP for an assignment through group membership."),
            string("status")
                .description("The status of the app user."),
            string("syncState")
                .description("The synchronization state of the app user."),
            string("created")
                .description("ISO 8601 timestamp for when the app user was created."),
            string("lastUpdated")
                .description("ISO 8601 timestamp for when the app user was last updated."),
            string("lastSync")
                .description("ISO 8601 timestamp of the last synchronization of the app user."),
            string("statusChanged")
                .description("ISO 8601 timestamp for when the status of the app user last changed."),
            object("credentials")
                .description("The app credentials of the user.")
                .properties(
                    string("userName")
                        .description("The username of the user in the app.")),
            object(PROFILE)
                .description("The app-specific profile properties of the user."));

    private OktaConstants() {
    }
}
