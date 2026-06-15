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

package com.bytechef.component.zendesk.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.definition.Context.Http.ResponseType;
import static com.bytechef.component.definition.Context.Http.responseType;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.EMAIL;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.NAME;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.ROLE;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.SKIP_VERIFY_EMAIL;
import static com.bytechef.component.zendesk.constant.ZendeskConstants.USER;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.TypeReference;
import java.util.Map;

/**
 * @author Nikolina Spehar
 */
public class ZendeskCreateUserAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("createUser")
        .title("Create User")
        .description("Creates a user.")
        .properties(
            string(NAME)
                .label("Name")
                .description("The name of the user.")
                .required(true),
            string(EMAIL)
                .label("Email")
                .description("The email address of the user.")
                .required(true),
            string(ROLE)
                .label("Role")
                .description("The role that will be assigned to new user.")
                .options(
                    option("Admin", "admin"),
                    option("Agent", "agent"),
                    option("End user", "end-user"))
                .required(false)
                .defaultValue("end-user"),
            bool(SKIP_VERIFY_EMAIL)
                .label("Skip Verify Email")
                .description("Whether a verification mail will be sent to the new user.")
                .required(false)
                .defaultValue(true))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("id")
                            .description("Automatically assigned when the user is created"),
                        string("url")
                            .description("The user's API url"),
                        string("name")
                            .description("The user's name"),
                        string("email")
                            .description(
                                "The user's primary email address. *Writeable on create only. On update, a secondary " +
                                    "email is added. See Email Address"),
                        string("created_at")
                            .description("The time the user was created"),
                        string("updated_at")
                            .description("The time the user was last updated"),
                        string("time_zone")
                            .description("The user's time zone. See Time Zone"),
                        string("iana_time_zone")
                            .description("The time zone for the user"),
                        string("phone")
                            .description("The user's primary phone number. See Phone Number below"),
                        bool("shared_phone_number")
                            .description("Whether the phone number is shared or not. See Phone Number below"),
                        object("photo")
                            .description("The user's profile picture represented as an Attachment object"),
                        integer("locale_id")
                            .description("The user's language identifier"),
                        string("locale")
                            .description(
                                "The user's locale. A BCP-47 compliant tag for the locale. If both \"locale\" and " +
                                    "\"locale_id\" are present on create or update, \"locale_id\" is ignored and " +
                                    "only \"locale\" is used."),
                        integer("organization_id")
                            .description(
                                "The id of the user's organization. If the user has more than one organization " +
                                    "memberships, the id of the user's default organization. If updating, see " +
                                    "Organization ID"),
                        string("role")
                            .description("The user's role. Possible values are \"end-user\", \"agent\", or \"admin\""),
                        bool("verified")
                            .description("Any of the user's identities is verified. See User Identities"),
                        string("last_active")
                            .description("Last time the user was active."),
                        string("external_id")
                            .description(
                                "A unique identifier from another system. The API treats the id as case insensitive. " +
                                    "Example: \"ian1\" and \"IAN1\" are the same value."),
                        array("tags")
                            .description("The user's tags. Only present if your account has user tagging enabled")
                            .items(string()),
                        string("alias")
                            .description("An alias displayed to end users"),
                        bool("active")
                            .description("false if the user has been deleted"),
                        bool("shared")
                            .description(
                                "If the user is shared from a different Zendesk Support instance. Shared users can " +
                                    "be added to organizations but cannot be modified through update requests. Any " +
                                    "attempt to update a shared user results in a 403 Forbidden error. Ticket " +
                                    "sharing accounts only"),
                        bool("shared_agent")
                            .description(
                                "If the user is a shared agent from a different Zendesk Support instance. Ticket " +
                                    "sharing accounts only"),
                        string("last_login_at")
                            .description(
                                "Last time the user signed in to Zendesk Support or made an API request using an " +
                                    "API token"),
                        bool("two_factor_auth_enabled")
                            .description("If two factor authentication is enabled"),
                        string("signature")
                            .description("The user's signature. Only agents and admins can have signatures"),
                        string("details")
                            .description("Any details you want to store about the user, such as an address"),
                        string("notes")
                            .description("Any notes you want to store about the user"),
                        integer("role_type")
                            .description(
                                "The user's role id. 0 for a custom agent, 1 for a light agent, 2 for a chat agent, " +
                                    "3 for a chat agent added to the Support account as a contributor (Chat Phase 4)," +
                                    " 4 for an admin, and 5 for a billing admin"),
                        integer("custom_role_id")
                            .description("A custom role if the user is an agent on the Enterprise plan or above"),
                        bool("is_billing_admin")
                            .description("Whether the user is a billing admin."),
                        bool("moderator")
                            .description("Designates whether the user has forum moderation capabilities"),
                        string("ticket_restriction")
                            .description(
                                "Specifies which tickets the user has access to. Possible values are: " +
                                    "\"organization\", \"groups\", \"assigned\", \"requested\", null. \"groups\" " +
                                    "and \"assigned\" are valid only for agents. If you pass an invalid value to an " +
                                    "end user (for example, \"groups\"), they will be assigned to \"requested\", " +
                                    "regardless of their previous access"),
                        bool("only_private_comments")
                            .description("true if the user can only create private comments"),
                        bool("restricted_agent")
                            .description(
                                "If the agent has any restrictions; false for admins and unrestricted agents, true " +
                                    "for other agents"),
                        bool("suspended")
                            .description(
                                "If the agent is suspended. Tickets from suspended users are also suspended, and " +
                                    "these users cannot sign in to the end user portal"),
                        integer("default_group_id")
                            .description("The id of the user's default group"),
                        bool("report_csv")
                            .description(
                                "This parameter is inert and has no effect. It may be deprecated in the future. " +
                                    "Previously, this parameter determined whether a user could access a CSV report " +
                                    "in a legacy Guide dashboard. This dashboard has been removed. See Announcing " +
                                    "Guide legacy reporting upgrade to Explore"),
                        object("user_fields")
                            .description("Values of custom fields in the user's profile. See User Fields"),
                        object("suspension_details")
                            .description(
                                "Channel-level suspension state for the user. The value is null if the user has no " +
                                    "active channel-level suspension"))))
        .help("", "https://docs.bytechef.io/reference/components/zendesk_v1#create-user")
        .perform(ZendeskCreateUserAction::perform);

    private ZendeskCreateUserAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Map<String, Object>> response = context.http(http -> http.post("/users"))
            .body(
                Body.of(
                    USER, Map.of(
                        EMAIL, inputParameters.getRequiredString(EMAIL),
                        NAME, inputParameters.getRequiredString(NAME),
                        ROLE, inputParameters.getString(ROLE),
                        SKIP_VERIFY_EMAIL, inputParameters.getBoolean(SKIP_VERIFY_EMAIL))))
            .configuration(responseType(ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return response.get(USER);
    }
}
