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

package com.bytechef.component.google.workspace.admin.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.number;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Marija Horvat
 */
public class GoogleWorkspaceAdminConstants {

    public static final String ADDRESS = "addresses";
    public static final String CHANGE_PASSWORD = "changePasswordAtNextLogin";
    public static final String EMAIL = "primaryEmail";
    public static final String FIRST_NAME = "givenName";
    public static final String LAST_NAME = "familyName";
    public static final String MAX_RESULTS = "maxResults";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String PAGE_TOKEN = "pageToken";
    public static final String PASSWORD = "password";
    public static final String PHONE = "phones";
    public static final String PRODUCT_ID = "productId";
    public static final String ROLE_ID = "roleId";
    public static final String SKU_ID = "skuId";
    public static final String USER_ID = "userId";

    public static final ModifiableObjectProperty LICENSE_OUTPUT_PROPERTY =
        object()
            .properties(
                string("kind")
                    .description("Identifies the resource as a LicenseAssignment."),
                string("etags")
                    .description("ETag of the resource."),
                string("productId")
                    .description("A product's unique identifier."),
                string("userId")
                    .description("The user's current primary email address."),
                string("selfLink")
                    .description("Link to this page."),
                string("skuId")
                    .description("A product SKU's unique identifier."),
                string("skuName")
                    .description("Display Name of the sku of the product."),
                string("productName")
                    .description("Display Name of the product."));

    public static final ModifiableObjectProperty ROLE_OUTPUT_PROPERTY =
        object()
            .properties(
                string("roleAssignmentId")
                    .description("ID of this roleAssignment."),
                string("roleId")
                    .description("The ID of the role that is assigned."),
                string("kind")
                    .description("The type of the API resource."),
                string("etag")
                    .description("ETag of the resource."),
                string("assignedTo")
                    .description("The unique ID of the entity this role is assigned to."),
                string("assigneeType")
                    .description("The type of the assignee."),
                string("scopeType")
                    .description("The scope in which this role is assigned."),
                string("orgUnitId")
                    .description(
                        "If the role is restricted to an organization unit, this contains the ID for the organization unit the exercise of this role is restricted to."),
                string("condition")
                    .description("The condition associated with this role assignment."));

    public static final ModifiableObjectProperty USER_OUTPUT_PROPERTY =
        object()
            .properties(
                string("id")
                    .description("The unique ID for the user."),
                string("primaryEmail")
                    .description("The user's primary email address."),
                string("password")
                    .description("The password for the user account."),
                string("hashFunction")
                    .description("The hash format of the password property."),
                bool("isAdmin")
                    .description("Indicates a user with super administrator privileges."),
                bool("isDelegatedAdmin")
                    .description("Indicates if the user is a delegated administrator."),
                bool("agreedToTerms")
                    .description(
                        "Indicates if the user has completed an initial login and accepted the Terms of Service agreement."),
                bool("suspended")
                    .description("Indicates if user is suspended."),
                bool("changePasswordAtNextLogin")
                    .description("Indicates if the user is forced to change their password at next login."),
                bool("ipWhitelisted")
                    .description(
                        "If true, the user's IP address is subject to a deprecated IP address allowlist configuration."),
                object("name")
                    .properties(
                        string("fullName")
                            .description(
                                "The user's full name formed by concatenating the first and last name values."),
                        string("familyName")
                            .description("The user's last name."),
                        string("givenName")
                            .description("The user's first name."),
                        string("displayName")
                            .description("The user's display name.")),
                string("kind")
                    .description("The type of the API resource."),
                string("etag")
                    .description("ETag of the resource."),
                array("emails")
                    .items(
                        object()
                            .properties(
                                string("address")
                                    .description("The user's email address."),
                                string("customType")
                                    .description(
                                        "If the email address type is custom, this property contains the custom value and must be set."),
                                bool("primary")
                                    .description("Indicates if this is the user's primary email."),
                                string("type")
                                    .description("The type of the email account."))),
                array("externalIds")
                    .items(
                        object()
                            .properties(
                                string("customType")
                                    .description(
                                        "If the external ID type is custom, this property contains the custom value and must be set."),
                                string("type")
                                    .description("The type of external ID."),
                                string("value")
                                    .description("The value of the external ID."))),
                array("relations")
                    .items(
                        object()
                            .properties(
                                string("customType")
                                    .description(
                                        "If the relationship type is custom, this property contains the custom value and must be set."),
                                string("type")
                                    .description("The type of relationship."),
                                string("value")
                                    .description("The email address of the person the user is related to."))),
                array("aliases")
                    .description("The list of the user's alias email addresses.")
                    .items(string()),
                bool("isMailboxSetup")
                    .description("Indicates if the user's Google mailbox is created."),
                string("customerId")
                    .description("The customer ID to retrieve all account users."),
                array("addresses")
                    .items(
                        object()
                            .properties(
                                string("country")
                                    .description("Country."),
                                string("countryCode")
                                    .description("The country code."),
                                string("customType")
                                    .description(
                                        "If the address type is custom, this property contains the custom value and must be set."),
                                string("extendedAddress")
                                    .description(
                                        "For extended addresses, such as an address that includes a sub-region."),
                                string("formatted")
                                    .description("A full and unstructured postal address."),
                                string("locality")
                                    .description("The town or city of the address."),
                                string("poBox")
                                    .description("The post office box, if present."),
                                string("postalCode")
                                    .description("The ZIP or postal code, if applicable."),
                                bool("primary")
                                    .description("If this is the user's primary address."),
                                string("region")
                                    .description("The abbreviated province or state."),
                                bool("sourceIsStructured")
                                    .description("Indicates if the user-supplied address was formatted."),
                                string("streetAddress")
                                    .description("The street address."),
                                string("type")
                                    .description("The address type."))),
                array("organizations")
                    .items(
                        object()
                            .properties(
                                string("costCenter")
                                    .description("The cost center of the user's organization."),
                                string("customType")
                                    .description(
                                        "If the value of type is custom, this property contains the custom type."),
                                string("department")
                                    .description(
                                        "Specifies the department within the organization, such as sales or engineering."),
                                string("description")
                                    .description("The description of the organization."),
                                string("domain")
                                    .description("The domain the organization belongs to."),
                                integer("fullTimeEquivalent")
                                    .description("The full-time equivalent millipercent within the organization"),
                                string("location")
                                    .description("The physical location of the organization."),
                                string("name")
                                    .description("The name of the organization."),
                                bool("primary")
                                    .description("Indicates if this is the user's primary organization."),
                                string("symbol")
                                    .description("Text string symbol of the organization."),
                                string("title")
                                    .description("The user's title within the organization."),
                                string("type")
                                    .description("The type of organization."))),
                string("lastLoginTime")
                    .description("The last time the user logged into the user's account."),
                array("phones")
                    .items(
                        object()
                            .properties(
                                string("customType")
                                    .description(
                                        "If the phone number type is custom, this property contains the custom value and must be set."),
                                bool("primary")
                                    .description("If true, this is the user's primary phone number."),
                                string("type")
                                    .description("The type of phone number."),
                                string("value")
                                    .description("A human-readable phone number."))),
                string("suspensionReason")
                    .description(
                        "Has the reason a user account is suspended either by the administrator or by Google at the time of suspension."),
                string("thumbnailPhotoUrl")
                    .description("The URL of the user's profile photo."),
                array("languages")
                    .items(
                        object()
                            .properties(
                                string("customLanguage")
                                    .description("Other language."),
                                string("languageCode")
                                    .description("ISO 639 string representation of a language."),
                                string("preference")
                                    .description(
                                        "If present, controls whether the specified languageCode is the user's preferred language."))),
                array("posixAccounts")
                    .items(
                        object()
                            .properties(
                                string("accountId")
                                    .description("A POSIX account field identifier."),
                                string("gecos")
                                    .description("The GECOS (user information) for this account."),
                                number("gid")
                                    .description("The default group ID."),
                                string("homeDirectory")
                                    .description("The path to the home directory for this account."),
                                string("operatingSystemType")
                                    .description("The operating system type for this account."),
                                bool("primary")
                                    .description("If this is user's primary account within the SystemId."),
                                string("shell")
                                    .description("The path to the login shell for this account."),
                                string("systemId")
                                    .description("System identifier for which account Username or Uid apply to."),
                                number("uid")
                                    .description("The POSIX compliant user ID."),
                                string("username")
                                    .description("The username of the account."))),
                string("creationTime")
                    .description("The time the user's account was created."),
                array("nonEditableAliases")
                    .description("The list of the user's non-editable alias email addresses.")
                    .items(string()),
                array("sshPublicKeys")
                    .items(
                        object()
                            .properties(
                                number("expirationTimeUsec")
                                    .description("An expiration time in microseconds since epoch."),
                                string("fingerprint")
                                    .description("A SHA-256 fingerprint of the SSH public key."),
                                string("key")
                                    .description("An SSH public key."))),
                object("notes")
                    .properties(
                        string("contentType")
                            .description("Content type of note, either plain text or HTML."),
                        string("value")
                            .description("Contents of notes.")),
                array("websites")
                    .items(
                        object()
                            .properties(
                                string("customType")
                                    .description(
                                        "If the website type is custom, this property contains the custom value and must be set."),
                                bool("primary")
                                    .description("If true, this is the user's primary website."),
                                string("type")
                                    .description("The type or purpose of the website."),
                                string("value")
                                    .description("The URL of the website."))),
                array("locations")
                    .items(
                        object()
                            .properties(
                                string("area")
                                    .description("Textual location."),
                                string("buildingId")
                                    .description("Building identifier."),
                                string("customType")
                                    .description(
                                        "If the location type is custom, this property contains the custom value and must be set."),
                                string("deskCode")
                                    .description("Most specific textual code of individual desk location."),
                                string("floorName")
                                    .description("Floor name/number."),
                                string("floorSection")
                                    .description("Floor section."),
                                string("type")
                                    .description("The location type."))),
                bool("includeInGlobalAddressList")
                    .description(
                        "Indicates if the user's profile is visible in the Google Workspace global address list when the contact sharing feature is enabled for the domain."),
                array("keywords")
                    .items(
                        object()
                            .properties(
                                string("customType")
                                    .description(
                                        "If the keyword type is custom, this property contains the custom value and must be set."),
                                string("type")
                                    .description(
                                        "Each entry can have a type which indicates standard type of that entry."),
                                string("value")
                                    .description("Keyword."))),
                string("deletionTime")
                    .description("The time the user's account was deleted."),
                object("gender")
                    .properties(
                        string("addressMeAs")
                            .description(
                                "A human-readable string containing the proper way to refer to the profile owner by humans."),
                        string("customGender")
                            .description("Name of a custom gender."),
                        string("type")
                            .description("The type of gender.")),
                string("thumbnailPhotoEtag")
                    .description("ETag of the user's photo"),
                array("ims")
                    .items(
                        object()
                            .properties(
                                string("customProtocol")
                                    .description(
                                        "If the protocol value is custom_protocol, this property holds the custom protocol's string."),
                                string("customType")
                                    .description(
                                        "If the IM type is custom, this property contains the custom value and must be set."),
                                string("im")
                                    .description("The user's IM network ID."),
                                bool("primary")
                                    .description("If this is the user's primary IM."),
                                string("protocol")
                                    .description("An IM protocol identifies the IM network."),
                                string("type")
                                    .description("The type of IM account."))),
                object("customSchemas")
                    .description("Custom fields of the user."),
                bool("isEnrolledIn2Sv")
                    .description("Is enrolled in 2-step verification."),
                bool("isEnforcedIn2Sv")
                    .description("Is 2-step verification enforced"),
                bool("archived")
                    .description("Indicates if user is archived."),
                string("orgUnitPath")
                    .description("The full path of the parent organization associated with the user."),
                string("recoveryEmail")
                    .description("Recovery email of the user."),
                string("recoveryPhone")
                    .description("Recovery phone of the user."));

    private GoogleWorkspaceAdminConstants() {
    }
}
