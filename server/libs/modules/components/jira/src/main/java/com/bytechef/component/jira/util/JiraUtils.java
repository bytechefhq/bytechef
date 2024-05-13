/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.component.jira.util;

import static com.bytechef.component.jira.constant.JiraConstants.NAME;
import static com.bytechef.component.jira.constant.JiraConstants.PROJECT;
import static com.bytechef.component.jira.constant.JiraConstants.YOUR_DOMAIN;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Parameters;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class JiraUtils {

    private JiraUtils() {
    }

    public static String getBaseUrl(Parameters connectionParameters) {
        return "https://" + connectionParameters.getRequiredString(YOUR_DOMAIN) + ".atlassian.net/rest/api/3";
    }

    public static String getProjectName(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) {

        Map<String, Object> body = context
            .http(http -> http.get(
                getBaseUrl(connectionParameters) + "/project/" + inputParameters.getRequiredString(PROJECT)))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        return (String) body.get(NAME);
    }

}
