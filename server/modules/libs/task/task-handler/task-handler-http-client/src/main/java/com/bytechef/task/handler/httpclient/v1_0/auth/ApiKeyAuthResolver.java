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

package com.bytechef.task.handler.httpclient.v1_0.auth;

import static com.bytechef.task.handler.httpclient.HTTPClientTaskConstants.*;

import com.bytechef.atlas.Accessor;
import com.bytechef.hermes.auth.domain.Authentication;
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
            Authentication authentication) {
        Accessor properties = authentication.getProperties();

        if (ApiTokenLocation.valueOf(StringUtils.upperCase(properties.getString(ADD_TO, "HEADER")))
                == ApiTokenLocation.HEADER) {
            headers.put(properties.getString(KEY, "api_token"), List.of(properties.getString(VALUE, "")));
        } else {
            queryParams.put(properties.getString(KEY, "api_token"), List.of(properties.getString(VALUE, "")));
        }
    }
}
