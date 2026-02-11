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

package com.bytechef.component.jira.constant;

import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.definition.ComponentDsl.ModifiableObjectProperty;

/**
 * @author Monika Kušter
 */
public class JiraConstants {

    public static final String ASSIGNEE = "assignee";
    public static final String ADD = "add";
    public static final String ADD_LABELS = "addLabels";
    public static final String ACCOUNT_ID = "accountId";
    public static final String COMMENT = "comment";
    public static final String CONTENT = "content";
    public static final String DESCRIPTION = "description";
    public static final String FIELDS = "fields";
    public static final String ID = "id";
    public static final String ISSUE = "issue";
    public static final String ISSUES = "issues";
    public static final String ISSUE_ID = "issueId";
    public static final String ISSUETYPE = "issuetype";
    public static final String JQL = "jql";
    public static final String KEY = "key";
    public static final String MAX_RESULTS = "maxResults";
    public static final String NAME = "name";
    public static final String NEXT_PAGE = "nextPage";
    public static final String NEXT_PAGE_TOKEN = "nextPageToken";
    public static final String ORDER_BY = "orderBy";
    public static final String PARENT = "parent";
    public static final String PRIORITY = "priority";
    public static final String PROJECT = "project";
    public static final String REMOVE = "remove";
    public static final String REMOVE_LABELS = "removeLabels";
    public static final String SELF = "self";
    public static final String STATUS_ID = "statusId";
    public static final String SUMMARY = "summary";
    public static final String TEXT = "text";
    public static final String TRANSITION = "transition";
    public static final String TYPE = "type";
    public static final String UPDATE = "update";

    public static final ModifiableObjectProperty COMMENT_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The ID of the comment."),
            string(SELF)
                .description("The URL of the comment."),
            object("author")
                .properties(
                    string(ACCOUNT_ID)
                        .description(
                            "The account ID of the user, which uniquely identifies the user across all Atlassian products."),
                    string("accountType")
                        .description("The type of account represented by this user."),
                    bool("active")
                        .description("Whether the user is active."),
                    object("avatarUrls")
                        .properties(
                            string("16x16")
                                .description("The URL of the item's 16x16 pixel avatar."),
                            string("24x24")
                                .description("The URL of the item's 24x24 pixel avatar."),
                            string("32x32")
                                .description("The URL of the item's 32x32 pixel avatar."),
                            string("48x48")
                                .description("The URL of the item's 48x48 pixel avatar."))),
            string("displayName")
                .description("The display name of the user."),
            string("emailAddress")
                .description("The email address of the user."),
            string(SELF)
                .description("The URL of the user."),
            string("timeZone")
                .description("The time zone specified in the user's profile."),
            object("body")
                .description("The comment text in Atlassian Document Format."),
            string("created")
                .description("The date and time at which the comment was created."),
            bool("jsdAuthorCanSeeRequest")
                .description(
                    "Whether the comment was added from an email sent by a person who is not part of the issue."),
            bool("jsdPublic")
                .description("Whether the comment is visible in Jira Service Desk."),
            array("properties")
                .items(
                    object()
                        .properties(
                            string(KEY)
                                .description("The key of the property."),
                            object("value")
                                .description("The value of the property."))),
            string("renderedBody")
                .description("The rendered version of the comment."),
            object("updateAuthor")
                .properties(
                    string(ACCOUNT_ID)
                        .description(
                            "The account ID of the user, which uniquely identifies the user across all Atlassian products."),
                    string("accountType")
                        .description("The type of account represented by this user."),
                    bool("active")
                        .description("Whether the user is active."),
                    object("avatarUrls")
                        .properties(
                            string("16x16")
                                .description("The URL of the item's 16x16 pixel avatar."),
                            string("24x24")
                                .description("The URL of the item's 24x24 pixel avatar."),
                            string("32x32")
                                .description("The URL of the item's 32x32 pixel avatar."),
                            string("48x48")
                                .description("The URL of the item's 48x48 pixel avatar."),
                            string("displayName")
                                .description("The display name of the user."),
                            string("emailAddress")
                                .description("The email address of the user."),
                            string(SELF)
                                .description("The URL of the user."),
                            string("timeZone")
                                .description("The time zone specified in the user's profile."))),
            string("updated")
                .description("The date and time at which the comment was updated last."),
            object("visibility")
                .properties(
                    string("identifier")
                        .description(
                            "The ID of the group or the name of the role that visibility of this item is restricted to."),
                    string("type")
                        .description("Whether visibility of this item is restricted to a group or role."),
                    string("value")
                        .description("The name of the group or role that visibility of this item is restricted to.")));

    public static final ModifiableObjectProperty ISSUE_OUTPUT_PROPERTY = object()
        .properties(
            string(ID)
                .description("The ID of the issue."),
            string(KEY)
                .description("The key of the issue."),
            string(SELF)
                .description("The URL of the issue details."),
            object(FIELDS)
                .properties(
                    object(ISSUETYPE)
                        .properties(
                            string(ID)
                                .description("ID of the issue type."),
                            string(NAME)
                                .description("Name of the issue type.")),
                    object(PROJECT)
                        .properties(
                            string(ID)
                                .description("ID of the project."),
                            string(NAME)
                                .description("Name of the project.")),
                    object(PRIORITY)
                        .properties(
                            string(ID)
                                .description("ID of the priority."),
                            string(NAME)
                                .description("Name of the priority.")),
                    object(ASSIGNEE)
                        .properties(
                            string("accountId")
                                .description("Account ID of the assignee."),
                            string("displayName")
                                .description("Display name of the assignee.")),
                    string(SUMMARY)
                        .description("Summary of the issue.")));

    public static final ModifiableObjectProperty LIST_ISSUE_OUTPUT_PROPERTY = object()
        .properties(
            integer("maxResults")
                .description("The maximum number of items that could be returned."),
            integer("startAt")
                .description("The index of the first item returned."),
            integer("total")
                .description("The number of items returned."),
            array("comments")
                .description("List of comments on the issue")
                .items(
                    object()
                        .properties(
                            string("id")
                                .description("The ID of the comment."),
                            string("self")
                                .description("The URL of the comment."),
                            object("body")
                                .properties(
                                    string("type")
                                        .description(
                                            "Defines the type of block node such as paragraph, table, and alike."),
                                    integer("version")
                                        .description(
                                            "Defines the version of ADF used in this representation."),
                                    array("content")
                                        .description(
                                            "An array containing inline and block nodes that define the content of a section of the document.")
                                        .items(
                                            object()
                                                .properties(string("text")))),
                            object("author")
                                .properties(
                                    string("accountId")
                                        .description(
                                            "The account ID of the user, which uniquely identifies the user across all Atlassian products."),
                                    string("accountType")
                                        .description(
                                            "The type of account represented by this user. This will be one of 'atlassian' (normal users), 'app' (application user) or 'customer' (Jira Service Desk customer user)"),
                                    bool("active")
                                        .description("Whether the user is active."),
                                    string("emailAddress")
                                        .description(
                                            "The email address of the user. Depending on the user’s privacy settings, this may be returned as null."),
                                    string("displayName")
                                        .description(
                                            "The display name of the user. Depending on the user’s privacy settings, this may return an alternative value."),
                                    string("self")
                                        .description("The URL of the user."),
                                    string("timezone")
                                        .description(
                                            "The time zone specified in the user's profile. Depending on the user’s privacy settings, this may be returned as null.")),
                            string("created")
                                .description("The date and time at which the comment was created."),
                            string("updated")
                                .description(
                                    "The date and time at which the comment was updated last."))));

    private JiraConstants() {
    }
}
