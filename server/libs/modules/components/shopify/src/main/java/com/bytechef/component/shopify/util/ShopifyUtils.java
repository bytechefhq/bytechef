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

package com.bytechef.component.shopify.util;

import static com.bytechef.component.shopify.constant.ShopifyConstants.QUERY;
import static com.bytechef.component.shopify.constant.ShopifyConstants.VARIABLES;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Context.Http.Body;
import com.bytechef.component.definition.Context.Http.ResponseType;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Map;

/**
 * @author Monika Domiter
 * @author Nikolina Spehar
 */
public class ShopifyUtils {

    private ShopifyUtils() {
    }

    public static void checkForUserError(Object content) {
        if (content instanceof Map<?, ?> contentMap
            && contentMap.get("userErrors") instanceof List<?> userErrors
            && userErrors.getFirst() instanceof Map<?, ?> userError) {

            throw new ProviderException((String) userError.get("message"));
        }
    }

    public static Map<String, Object> sendGraphQlQuery(
        String query, Context context, Map<String, Object> variables) {

        Map<String, Object> body = context
            .http(http -> http.post("/2025-10/graphql.json"))
            .configuration(Http.responseType(ResponseType.JSON))
            .body(
                Body.of(
                    Map.of(
                        QUERY, query,
                        VARIABLES, variables)))
            .execute()
            .getBody(new TypeReference<>() {});

        checkIfErrorResponse(body);

        return (Map<String, Object>) body.get("data");
    }

    private static void checkIfErrorResponse(Map<String, Object> response) {
        if (response.get("errors") instanceof List<?> errors
            && errors.getFirst() instanceof Map<?, ?> error) {

            throw new ProviderException((String) error.get("message"));
        }
    }
}
