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

package com.bytechef.component.http.client.auth.resolver;

import static com.bytechef.component.http.client.constants.HttpClientConstants.ADD_TO;
import static com.bytechef.component.http.client.constants.HttpClientConstants.KEY;
import static com.bytechef.component.http.client.constants.HttpClientConstants.VALUE;

import com.bytechef.component.http.client.constants.HttpClientConstants;
import com.bytechef.component.http.client.constants.HttpClientConstants.ApiTokenLocation;
import com.bytechef.hermes.component.ConnectionParameters;
import com.github.mizosoft.methanol.Methanol;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Ivica Cardic
 */
public class ApiKeyAuthResolver implements AuthResolver {

    @Override
    public void apply(
            Methanol.Builder builder,
            Map<String, List<String>> headers,
            Map<String, List<String>> queryParams,
            ConnectionParameters connectionParameters) {

        if (ApiTokenLocation.valueOf(StringUtils.upperCase(
                        connectionParameters.getParameter(ADD_TO, ApiTokenLocation.HEADER.name())))
                == ApiTokenLocation.HEADER) {
            headers.put(
                    connectionParameters.getParameter(KEY, HttpClientConstants.API_TOKEN),
                    List.of(connectionParameters.getParameter(VALUE, "")));
        } else {
            queryParams.put(
                    connectionParameters.getParameter(KEY, HttpClientConstants.API_TOKEN),
                    List.of(connectionParameters.getParameter(VALUE, "")));
        }
    }
}
