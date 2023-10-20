
/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.jira.action;

import static com.bytechef.hermes.component.OpenApiComponentHandler.PropertyType;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.array;
import static com.bytechef.hermes.component.definition.ComponentDSL.bool;
import static com.bytechef.hermes.component.definition.ComponentDSL.object;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static com.bytechef.hermes.component.util.HttpClientUtils.ResponseFormat;

import com.bytechef.component.jira.property.JiraIssueBeanProperties;
import com.bytechef.hermes.component.definition.ComponentDSL;
import java.util.Map;

/**
 * Provides a list of the component actions.
 *
 * @generated
 */
public class JiraGetIssueAction {
    public static final ComponentDSL.ModifiableActionDefinition ACTION_DEFINITION = action("getIssue")
        .title("Get issue")
        .description("Returns the details for an issue.\n"
            + "\n"
            + "The issue is identified by its ID or key, however, if the identifier doesn't match an issue, a case-insensitive search and check for moved issues is performed. If a matching issue is found its details are returned, a 302 or other redirect is **not** returned. The issue key returned in the response is the key of the issue found.\n"
            + "\n"
            + "This operation can be accessed anonymously.\n"
            + "\n"
            + "**[Permissions](#permissions) required:**\n"
            + "\n"
            + " *  *Browse projects* [project permission](https://confluence.atlassian.com/x/yodKLg) for the project that the issue is in.\n"
            + " *  If [issue-level security](https://confluence.atlassian.com/x/J4lKLg) is configured, issue-level security permission to view the issue.")
        .metadata(
            Map.of(
                "method", "GET",
                "path", "/rest/api/3/issue/{issueIdOrKey}"

            ))
        .properties(string("issueIdOrKey").label("Issue Id Or Key")
            .description("The ID or key of the issue.")
            .required(true)
            .metadata(
                Map.of(
                    "type", PropertyType.PATH)),
            array("fields").items(string())
                .placeholder("Add")
                .label("Fields")
                .description(
                    "A list of fields to return for the issue. This parameter accepts a comma-separated list. Use it to retrieve a subset of fields. Allowed values:\n"
                        + "\n"
                        + " *  `*all` Returns all fields.\n"
                        + " *  `*navigable` Returns navigable fields.\n"
                        + " *  Any issue field, prefixed with a minus to exclude.\n"
                        + "\n"
                        + "Examples:\n"
                        + "\n"
                        + " *  `summary,comment` Returns only the summary and comments fields.\n"
                        + " *  `-description` Returns all (default) fields except description.\n"
                        + " *  `*navigable,-comment` Returns all navigable fields except comment.\n"
                        + "\n"
                        + "This parameter may be specified multiple times. For example, `fields=field1,field2& fields=field3`.\n"
                        + "\n"
                        + "Note: All fields are returned by default. This differs from [Search for issues using JQL (GET)](#api-rest-api-3-search-get) and [Search for issues using JQL (POST)](#api-rest-api-3-search-post) where the default is all navigable fields.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            bool("fieldsByKeys").label("Fields By Keys")
                .description(
                    "Whether fields in `fields` are referenced by keys rather than IDs. This parameter is useful where fields have been added by a connect app and a field's key may differ from its ID.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            string("expand").label("Expand")
                .description(
                    "Use [expand](#expansion) to include additional information about the issues in the response. This parameter accepts a comma-separated list. Expand options include:\n"
                        + "\n"
                        + " *  `renderedFields` Returns field values rendered in HTML format.\n"
                        + " *  `names` Returns the display name of each field.\n"
                        + " *  `schema` Returns the schema describing a field type.\n"
                        + " *  `transitions` Returns all possible transitions for the issue.\n"
                        + " *  `editmeta` Returns information about how each field can be edited.\n"
                        + " *  `changelog` Returns a list of recent updates to an issue, sorted by date, starting from the most recent.\n"
                        + " *  `versionedRepresentations` Returns a JSON array for each version of a field's value, with the highest number representing the most recent version. Note: When included in the request, the `fields` parameter is ignored.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            array("properties").items(string())
                .placeholder("Add")
                .label("Properties")
                .description(
                    "A list of issue properties to return for the issue. This parameter accepts a comma-separated list. Allowed values:\n"
                        + "\n"
                        + " *  `*all` Returns all issue properties.\n"
                        + " *  Any issue property key, prefixed with a minus to exclude.\n"
                        + "\n"
                        + "Examples:\n"
                        + "\n"
                        + " *  `*all` Returns all properties.\n"
                        + " *  `*all,-prop1` Returns all properties except `prop1`.\n"
                        + " *  `prop1,prop2` Returns `prop1` and `prop2` properties.\n"
                        + "\n"
                        + "This parameter may be specified multiple times. For example, `properties=prop1,prop2& properties=prop3`.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)),
            bool("updateHistory").label("Update History")
                .description(
                    "Whether the project in which the issue is created is added to the user's **Recently viewed** project list, as shown under **Projects** in Jira. This also populates the [JQL issues search](#api-rest-api-3-search-get) `lastViewed` field.")
                .required(false)
                .metadata(
                    Map.of(
                        "type", PropertyType.QUERY)))
        .outputSchema(object().properties(JiraIssueBeanProperties.PROPERTIES)
            .description("Details about an issue.")
            .metadata(
                Map.of(
                    "responseFormat", ResponseFormat.JSON)))
        .sampleOutput(
            "{\"id\":\"10002\",\"self\":\"https://your-domain.atlassian.net/rest/api/3/issue/10002\",\"key\":\"ED-1\",\"fields\":{\"watcher\":{\"self\":\"https://your-domain.atlassian.net/rest/api/3/issue/EX-1/watchers\",\"isWatching\":false,\"watchCount\":1,\"watchers\":[{\"self\":\"https://your-domain.atlassian.net/rest/api/3/user?accountId=5b10a2844c20165700ede21g\",\"accountId\":\"5b10a2844c20165700ede21g\",\"displayName\":\"Mia Krystof\",\"active\":false}]},\"attachment\":[{\"id\":10000,\"self\":\"https://your-domain.atlassian.net/rest/api/3/attachments/10000\",\"filename\":\"picture.jpg\",\"author\":{\"self\":\"https://your-domain.atlassian.net/rest/api/3/user?accountId=5b10a2844c20165700ede21g\",\"key\":\"\",\"accountId\":\"5b10a2844c20165700ede21g\",\"accountType\":\"atlassian\",\"name\":\"\",\"avatarUrls\":{\"48x48\":\"https://avatar-management--avatars.server-location.prod.public.atl-paas.net/initials/MK-5.png?size=48&s=48\",\"24x24\":\"https://avatar-management--avatars.server-location.prod.public.atl-paas.net/initials/MK-5.png?size=24&s=24\",\"16x16\":\"https://avatar-management--avatars.server-location.prod.public.atl-paas.net/initials/MK-5.png?size=16&s=16\",\"32x32\":\"https://avatar-management--avatars.server-location.prod.public.atl-paas.net/initials/MK-5.png?size=32&s=32\"},\"displayName\":\"Mia Krystof\",\"active\":false},\"created\":\"2022-11-30T07:18:19.441+0000\",\"size\":23123,\"mimeType\":\"image/jpeg\",\"content\":\"https://your-domain.atlassian.net/jira/rest/api/3/attachment/content/10000\",\"thumbnail\":\"https://your-domain.atlassian.net/jira/rest/api/3/attachment/thumbnail/10000\"}],\"sub-tasks\":[{\"id\":\"10000\",\"type\":{\"id\":\"10000\",\"name\":\"\",\"inward\":\"Parent\",\"outward\":\"Sub-task\"},\"outwardIssue\":{\"id\":\"10003\",\"key\":\"ED-2\",\"self\":\"https://your-domain.atlassian.net/rest/api/3/issue/ED-2\",\"fields\":{\"status\":{\"iconUrl\":\"https://your-domain.atlassian.net/images/icons/statuses/open.png\",\"name\":\"Open\"}}}}],\"description\":{\"type\":\"doc\",\"version\":1,\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Main order flow broken\"}]}]},\"project\":{\"self\":\"https://your-domain.atlassian.net/rest/api/3/project/EX\",\"id\":\"10000\",\"key\":\"EX\",\"name\":\"Example\",\"avatarUrls\":{\"48x48\":\"https://your-domain.atlassian.net/secure/projectavatar?size=large&pid=10000\",\"24x24\":\"https://your-domain.atlassian.net/secure/projectavatar?size=small&pid=10000\",\"16x16\":\"https://your-domain.atlassian.net/secure/projectavatar?size=xsmall&pid=10000\",\"32x32\":\"https://your-domain.atlassian.net/secure/projectavatar?size=medium&pid=10000\"},\"projectCategory\":{\"self\":\"https://your-domain.atlassian.net/rest/api/3/projectCategory/10000\",\"id\":\"10000\",\"name\":\"FIRST\",\"description\":\"First Project Category\"},\"simplified\":false,\"style\":\"classic\",\"insight\":{\"totalIssueCount\":100,\"lastIssueUpdateTime\":\"2022-11-30T07:18:17.152+0000\"}},\"comment\":[{\"self\":\"https://your-domain.atlassian.net/rest/api/3/issue/10010/comment/10000\",\"id\":\"10000\",\"author\":{\"self\":\"https://your-domain.atlassian.net/rest/api/3/user?accountId=5b10a2844c20165700ede21g\",\"accountId\":\"5b10a2844c20165700ede21g\",\"displayName\":\"Mia Krystof\",\"active\":false},\"body\":{\"type\":\"doc\",\"version\":1,\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Lorem ipsum dolor sit amet, consectetur adipiscing elit. Pellentesque eget venenatis elit. Duis eu justo eget augue iaculis fermentum. Sed semper quam laoreet nisi egestas at posuere augue semper.\"}]}]},\"updateAuthor\":{\"self\":\"https://your-domain.atlassian.net/rest/api/3/user?accountId=5b10a2844c20165700ede21g\",\"accountId\":\"5b10a2844c20165700ede21g\",\"displayName\":\"Mia Krystof\",\"active\":false},\"created\":\"2021-01-17T12:34:00.000+0000\",\"updated\":\"2021-01-18T23:45:00.000+0000\",\"visibility\":{\"type\":\"role\",\"value\":\"Administrators\",\"identifier\":\"Administrators\"}}],\"issuelinks\":[{\"id\":\"10001\",\"type\":{\"id\":\"10000\",\"name\":\"Dependent\",\"inward\":\"depends on\",\"outward\":\"is depended by\"},\"outwardIssue\":{\"id\":\"10004L\",\"key\":\"PR-2\",\"self\":\"https://your-domain.atlassian.net/rest/api/3/issue/PR-2\",\"fields\":{\"status\":{\"iconUrl\":\"https://your-domain.atlassian.net/images/icons/statuses/open.png\",\"name\":\"Open\"}}}},{\"id\":\"10002\",\"type\":{\"id\":\"10000\",\"name\":\"Dependent\",\"inward\":\"depends on\",\"outward\":\"is depended by\"},\"inwardIssue\":{\"id\":\"10004\",\"key\":\"PR-3\",\"self\":\"https://your-domain.atlassian.net/rest/api/3/issue/PR-3\",\"fields\":{\"status\":{\"iconUrl\":\"https://your-domain.atlassian.net/images/icons/statuses/open.png\",\"name\":\"Open\"}}}}],\"worklog\":[{\"self\":\"https://your-domain.atlassian.net/rest/api/3/issue/10010/worklog/10000\",\"author\":{\"self\":\"https://your-domain.atlassian.net/rest/api/3/user?accountId=5b10a2844c20165700ede21g\",\"accountId\":\"5b10a2844c20165700ede21g\",\"displayName\":\"Mia Krystof\",\"active\":false},\"updateAuthor\":{\"self\":\"https://your-domain.atlassian.net/rest/api/3/user?accountId=5b10a2844c20165700ede21g\",\"accountId\":\"5b10a2844c20165700ede21g\",\"displayName\":\"Mia Krystof\",\"active\":false},\"comment\":{\"type\":\"doc\",\"version\":1,\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"I did some work here.\"}]}]},\"updated\":\"2021-01-18T23:45:00.000+0000\",\"visibility\":{\"type\":\"group\",\"value\":\"jira-developers\",\"identifier\":\"276f955c-63d7-42c8-9520-92d01dca0625\"},\"started\":\"2021-01-17T12:34:00.000+0000\",\"timeSpent\":\"3h 20m\",\"timeSpentSeconds\":12000,\"id\":\"100028\",\"issueId\":\"10002\"}],\"updated\":1,\"timetracking\":{\"originalEstimate\":\"10m\",\"remainingEstimate\":\"3m\",\"timeSpent\":\"6m\",\"originalEstimateSeconds\":600,\"remainingEstimateSeconds\":200,\"timeSpentSeconds\":400}}}");
}
