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

package com.bytechef.component.microsoft.one.drive.util;

import static com.bytechef.component.definition.ComponentDSL.option;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.BASE_URL;
import static com.bytechef.component.microsoft.one.drive.constant.MicrosoftOneDriveConstants.PARENT_ID;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.TypeReference;
import com.bytechef.component.definition.Option;
import com.bytechef.component.definition.Parameters;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 */
public class MicrosoftOneDriveUtils {

    private MicrosoftOneDriveUtils() {
    }

    public static List<Option<String>> getFileIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        Map<String, ?> body = context
            .http(http -> http.get(BASE_URL + "/items/" + getFolderId(inputParameters) + "/children"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("value") instanceof List<?> list) {
            for (Object item : list) {
                if ((item instanceof Map<?, ?> map) && (map.containsKey("file"))) {
                    options.add(option((String) map.get("name"), (String) map.get("id")));
                }
            }
        }

        return options;
    }

    public static String getFolderId(Parameters inputParameters) {
        String parentId = inputParameters.getString(PARENT_ID);

        return (parentId == null) ? "root" : parentId;
    }

    public static List<Option<String>> getFolderIdOptions(
        Parameters inputParameters, Parameters connectionParameters, String searchText, ActionContext context) {

        String encode = URLEncoder.encode("folder ne null", StandardCharsets.UTF_8);

        Map<String, ?> body = context
            .http(http -> http.get(BASE_URL + "/items/root/children?$filter=" + encode))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<>() {});

        List<Option<String>> options = new ArrayList<>();

        if (body.get("value") instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    options.add(option((String) map.get("name"), (String) map.get("id")));
                }
            }
        }

        return options;
    }

}
