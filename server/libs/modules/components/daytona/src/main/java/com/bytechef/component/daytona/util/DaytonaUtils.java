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

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.TypeReference;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public final class DaytonaUtils {

    private static final String CODE_TOOLBOX_LANGUAGE_LABEL = "code-toolbox-language";
    private static final Duration SANDBOX_START_POLL_INTERVAL = Duration.ofSeconds(1);
    private static final Duration SANDBOX_START_TIMEOUT = Duration.ofSeconds(60);
    private static final String STARTED_STATE = "started";

    private static final Logger log = LoggerFactory.getLogger(DaytonaUtils.class);

    private DaytonaUtils() {
    }

    public static String createSandbox(ActionContext context, String language) {
        Map<String, Object> body = Map.of();

        if (language != null && !language.isBlank()) {
            body = Map.of("labels", Map.of(CODE_TOOLBOX_LANGUAGE_LABEL, language));
        }

        Map<String, Object> sandbox = context
            .http(http -> http.post("/sandbox"))
            .body(Http.Body.of(body))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<Map<String, Object>>() {});

        Object id = sandbox.get("id");

        if (id == null) {
            throw new IllegalStateException("Daytona did not return a sandbox id");
        }

        String sandboxId = String.valueOf(id);

        waitUntilStarted(context, sandboxId, sandbox);

        return sandboxId;
    }

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

    public static String getToolboxUrl(ActionContext context, String sandboxId) {
        Map<String, Object> toolboxProxyUrl = context
            .http(http -> http.get("/sandbox/" + sandboxId + "/toolbox-proxy-url"))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<Map<String, Object>>() {});

        Object url = toolboxProxyUrl.get("url");

        if (url == null) {
            throw new IllegalStateException("Daytona did not return a toolbox URL for sandbox " + sandboxId);
        }

        String toolboxUrl = String.valueOf(url);

        if (toolboxUrl.endsWith("/")) {
            toolboxUrl = toolboxUrl.substring(0, toolboxUrl.length() - 1);
        }

        return toolboxUrl + "/" + sandboxId;
    }

    private static void waitUntilStarted(ActionContext context, String sandboxId, Map<String, Object> sandbox) {
        long deadline = System.nanoTime() + SANDBOX_START_TIMEOUT.toNanos();

        Map<String, Object> currentSandbox = sandbox;

        while (!STARTED_STATE.equalsIgnoreCase(String.valueOf(currentSandbox.get("state")))) {
            String state = String.valueOf(currentSandbox.get("state"));

            if ("error".equalsIgnoreCase(state) || "build_failed".equalsIgnoreCase(state)) {
                throw new IllegalStateException(
                    "Daytona sandbox " + sandboxId + " failed to start: " + currentSandbox.get("errorReason"));
            }

            if (System.nanoTime() > deadline) {
                throw new IllegalStateException(
                    "Daytona sandbox " + sandboxId + " did not start within " + SANDBOX_START_TIMEOUT.toSeconds() +
                        " seconds (state: " + state + ")");
            }

            sleep();

            currentSandbox = context
                .http(http -> http.get("/sandbox/" + sandboxId))
                .configuration(Http.responseType(Http.ResponseType.JSON))
                .execute()
                .getBody(new TypeReference<Map<String, Object>>() {});
        }
    }

    private static void sleep() {
        try {
            Thread.sleep(SANDBOX_START_POLL_INTERVAL);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException("Interrupted while waiting for the Daytona sandbox to start",
                interruptedException);
        }
    }
}
