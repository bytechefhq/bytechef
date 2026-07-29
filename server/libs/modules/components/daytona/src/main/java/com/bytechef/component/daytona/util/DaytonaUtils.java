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

package com.bytechef.component.daytona.util;

import static com.bytechef.component.daytona.constant.DaytonaConstants.LANGUAGE;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared Daytona sandbox lifecycle helpers used by the Daytona actions. The REST paths follow Daytona's documented
 * SDK/API conventions and are marked inferred where they could not be verified against the live API reference.
 *
 * @author Ivica Cardic
 */
public final class DaytonaUtils {

    private static final Logger log = LoggerFactory.getLogger(DaytonaUtils.class);

    private DaytonaUtils() {
    }

    /**
     * Creates a fresh sandbox and returns its id. Endpoint: {@code POST /sandbox}.
     */
    public static String createSandbox(ActionContext context, String language) {
        Map<String, Object> body = new LinkedHashMap<>();

        if (language != null && !language.isBlank()) {
            body.put(LANGUAGE, language);
        }

        Map<String, Object> response = context
            .http(http -> http.post("/sandbox"))
            .body(Http.Body.of(body))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<Map<String, Object>>() {});

        Object id = response.get("id");

        if (id == null) {
            throw new IllegalStateException("Daytona did not return a sandbox id");
        }

        return String.valueOf(id);
    }

    /**
     * Deletes a sandbox, returning whether the deletion succeeded. Never throws so it is safe in a {@code finally}
     * teardown. Endpoint: {@code DELETE /sandbox/{sandboxId}}.
     */
    public static boolean deleteSandbox(ActionContext context, String sandboxId) {
        try {
            context
                .http(http -> http.delete("/sandbox/" + sandboxId))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute();

            return true;
        } catch (Exception exception) {
            log.warn("Failed to delete Daytona sandbox {}", sandboxId, exception);

            return false;
        }
    }
}
