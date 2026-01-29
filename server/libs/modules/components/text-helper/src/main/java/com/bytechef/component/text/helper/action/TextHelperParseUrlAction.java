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

package com.bytechef.component.text.helper.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.text.helper.constant.TextHelperConstants.URL;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Nikolina Špehar
 */
public class TextHelperParseUrlAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("parseUrl")
        .title("Parse URL")
        .description("Parses URL into structured data.")
        .properties(
            string(URL)
                .label("URL")
                .description("The URL that will be turned into structured data.")
                .required(true))
        .output(outputSchema(
            object()
                .description("The parsed URL data.")
                .properties(
                    string("protocol")
                        .description("The protocol of the URL."),
                    string("slashes")
                        .description("Indicates if the URL has a slash after the protocol."),
                    string("auth")
                        .description("The authentication information of the URL."),
                    string("host")
                        .description("The host part of the URL."),
                    string("port")
                        .description("The port part of the URL."),
                    string("hostname")
                        .description("The hostname part of the URL."),
                    string("hash")
                        .description("The hash part of the URL."),
                    string("search")
                        .description("The search part of the URL."),
                    string("pathname")
                        .description("The pathname part of the URL."),
                    string("path")
                        .description("The path part of the URL including the query if present."),
                    string("href")
                        .description("The full URL as a string."),
                    object("query")
                        .description("The query parameters of the URL.")
                        .properties(
                            string("key")
                                .description("The key of the query parameter."),
                            string("value")
                                .description("The value of the query parameter.")))))
        .perform(TextHelperParseUrlAction::perform);

    private TextHelperParseUrlAction() {
    }

    public static Map<String, Object> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        Map<String, Object> result = new HashMap<>();

        String urlString = inputParameters.getRequiredString(URL);

        try {
            URI uri = URI.create(urlString);
            URL url = uri.toURL();

            result.put("protocol", url.getProtocol() + ":");
            result.put("slashes", true);
            result.put("auth", null);
            result.put("host", url.getHost() + ((url.getPort() != -1) ? ":" + url.getPort() : ""));
            result.put("port", (url.getPort() == -1) ? "" : String.valueOf(url.getPort()));
            result.put("hostname", url.getHost());
            result.put("hash", (url.getRef() == null) ? null : "#" + url.getRef());
            result.put("search", (url.getQuery() == null) ? null : "?" + url.getQuery());
            result.put("pathname", url.getPath());
            result.put("path", url.getPath() + ((url.getQuery() == null) ? "" : "?" + url.getQuery()));
            result.put("href", url.toString());

            Map<String, String> queryMap = new HashMap<>();

            if (url.getQuery() != null && !url.getQuery()
                .isEmpty()) {

                String[] pairs = url.getQuery()
                    .split("&");

                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
                    String value = (kv.length > 1) ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
                    queryMap.put(key, value);
                }
            }
            result.put("query", queryMap);

        } catch (Exception e) {
            throw new ProviderException("Invalid URL: " + e.getMessage());
        }

        return result;
    }
}
