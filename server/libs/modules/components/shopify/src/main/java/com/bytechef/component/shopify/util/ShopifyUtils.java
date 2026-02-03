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

    public static Object executeGraphQlOperation(
        String query, Context context, Map<String, Object> variables, String dataKey) {

        Object data = sendGraphQlQuery(query, context, variables);

        if (data instanceof Map<?, ?> dataMap) {
            Object content = dataMap.get(dataKey);

            checkForUserError(content);

            return content;
        }

        throw new ProviderException("GraphQL query was not executed correctly.");
    }

    public static Object sendGraphQlQuery(
        String query, Context context, Map<String, Object> variables) {

        Map<String, Object> body = context
            .http(http -> http.post("/2025-10/graphql.json"))
            .configuration(Http.responseType(ResponseType.JSON))
            .body(Body.of(QUERY, query, VARIABLES, variables))
            .execute()
            .getBody(new TypeReference<>() {});

        checkForErrors(body.get("errors"));

        return body.get("data");
    }

    private static void checkForUserError(Object content) {
        if (content instanceof Map<?, ?> contentMap) {
            Object userErrorsObj = contentMap.get("userErrors");

            checkForErrors(userErrorsObj);
        }
    }

    private static void checkForErrors(Object userErrorsObj) {
        if (userErrorsObj instanceof List<?> userErrors && !userErrors.isEmpty()) {
            Object first = userErrors.getFirst();

            if (first instanceof Map<?, ?> userError) {
                Object message = userError.get("message");

                if (message instanceof String msg && !msg.isBlank()) {
                    throw new ProviderException(msg);
                }
            }
        }
    }
}
