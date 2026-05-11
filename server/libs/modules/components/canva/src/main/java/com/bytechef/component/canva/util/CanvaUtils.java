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

package com.bytechef.component.canva.util;

import static com.bytechef.component.definition.Context.Http;

import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.TypeReference;
import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Map;

/**
 * @author Ivona Pavela
 */
public class CanvaUtils {

    private CanvaUtils() {
    }

    public static Map<String, Object> pollJob(Context context, String url) {
        Map<String, Object> response;
        String status;
        int delayMs = 5000;
        int maxAttempts = 10;
        int currentAttempt = 0;

        do {
            if (currentAttempt > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread()
                        .interrupt();

                    throw new ProviderException("Job polling interrupted: ", interruptedException);
                }
            }

            response = context
                .http(http -> http.get(url))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<>() {});

            status = (String) response.get("status");

            if (status.equals("success")) {
                return response;
            } else if (status.equals("failed") && response.get("error") instanceof Map<?, ?> error) {
                throw new ProviderException((String) error.get("message"));
            }

            currentAttempt++;
        } while (status.equals("in_progress") && currentAttempt < maxAttempts);

        return response;
    }

    public static ProviderException processErrorResponse(
        int statusCode, Object body, Map<String, List<String>> headers, Context context) {

        Object json = context.json(json1 -> json1.read((String) body));

        if (json instanceof Map<?, ?> errorMap && errorMap.containsKey("message")) {
            return new ProviderException(statusCode, (String) errorMap.get("message"));
        }

        return new ProviderException(statusCode, body.toString());
    }
}
