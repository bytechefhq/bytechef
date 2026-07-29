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

package com.bytechef.component.daytona.action;

import static com.bytechef.component.daytona.constant.DaytonaConstants.CODE;
import static com.bytechef.component.daytona.constant.DaytonaConstants.KEEP_SANDBOX;
import static com.bytechef.component.daytona.constant.DaytonaConstants.LANGUAGE;
import static com.bytechef.component.daytona.constant.DaytonaConstants.SANDBOX_ID;
import static com.bytechef.component.daytona.constant.DaytonaConstants.TIMEOUT;
import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.option;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;

import com.bytechef.component.daytona.util.DaytonaUtils;
import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context.Http;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.definition.Property.ControlType;
import com.bytechef.component.definition.TypeReference;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes a snippet of AI-generated code inside a Daytona sandbox and returns the result. Designed to be attached to
 * the AI Agent as a tool so the agent can generate code on the fly and run it safely.
 *
 * <p>
 * By default the action creates an ephemeral sandbox, runs the code, and deletes the sandbox afterward. To keep state
 * across turns, the agent can pass an existing {@code sandboxId} (from {@code createSandbox} or a prior run with
 * {@code keepSandbox=true}); a reused sandbox is never deleted by this action so the caller owns its lifecycle.
 * </p>
 *
 * <p>
 * The Daytona REST paths and request/response field names follow Daytona's documented SDK/API conventions. The
 * confirmed code-run response fields are {@code exitCode}, {@code result} (stdout), and {@code artifacts} (with
 * {@code stdout} and {@code charts}). Toolbox routing (the {@code /toolbox/{sandboxId}/toolbox/...} prefix) should be
 * verified against your account's API version.
 * </p>
 *
 * @author Ivica Cardic
 */
public class DaytonaExecuteCodeAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("executeCode")
        .title("Execute Code")
        .description(
            "Generates and runs code in a secure, isolated Daytona sandbox and returns its output. Use this to " +
                "execute AI-generated code (data analysis, calculations, scripts) safely and get the result back.")
        .properties(
            string(LANGUAGE)
                .label("Language")
                .description("The programming language of the code to execute.")
                .options(
                    option("Python", "python"),
                    option("TypeScript", "typescript"),
                    option("JavaScript", "javascript"),
                    option("Bash", "bash"))
                .defaultValue("python")
                .required(true),
            string(CODE)
                .label("Code")
                .description("The source code to execute in the sandbox.")
                .controlType(ControlType.CODE_EDITOR)
                .required(true),
            string(SANDBOX_ID)
                .label("Sandbox ID")
                .description(
                    "Run in an existing sandbox to preserve state across turns. Leave empty to create a fresh, " +
                        "ephemeral sandbox for this run. A reused sandbox is never deleted by this action.")
                .required(false),
            bool(KEEP_SANDBOX)
                .label("Keep Sandbox")
                .description(
                    "When creating a new sandbox, keep it alive after the run (instead of deleting it) so it can be " +
                        "reused via its returned Sandbox ID. Ignored when a Sandbox ID is supplied.")
                .defaultValue(false)
                .required(false),
            integer(TIMEOUT)
                .label("Timeout")
                .description("Maximum time in seconds to wait for the code to finish executing.")
                .defaultValue(60)
                .minValue(1)
                .maxValue(3600)
                .required(false))
        .output(
            outputSchema(
                object()
                    .properties(
                        integer("exitCode")
                            .description("The exit code of the executed code (0 indicates success)."),
                        string("stdout")
                            .description("The standard output produced by the code."),
                        bool("success")
                            .description("Whether the code finished with a zero exit code."),
                        string(SANDBOX_ID)
                            .description("The sandbox the code ran in (reuse it to preserve state)."),
                        array("charts")
                            .description("Chart artifacts (e.g. matplotlib) captured during execution, if any."))))
        .perform(DaytonaExecuteCodeAction::perform);

    private DaytonaExecuteCodeAction() {
    }

    public static Object perform(Parameters inputParameters, Parameters connectionParameters, ActionContext context) {
        String language = inputParameters.getString(LANGUAGE, "python");
        String code = inputParameters.getRequiredString(CODE);
        Integer timeout = inputParameters.getInteger(TIMEOUT);
        String providedSandboxId = inputParameters.getString(SANDBOX_ID);
        boolean keepSandbox = inputParameters.getBoolean(KEEP_SANDBOX, false);

        boolean reuse = providedSandboxId != null && !providedSandboxId.isBlank();
        String sandboxId = reuse ? providedSandboxId : DaytonaUtils.createSandbox(context, language);

        try {
            Map<String, Object> response = runCode(context, sandboxId, language, code, timeout);

            return toResult(response, sandboxId);
        } finally {
            // Only tear down sandboxes this action created and that the caller did not ask to keep. A reused sandbox
            // is owned by the caller and left running.
            if (!reuse && !keepSandbox) {
                DaytonaUtils.deleteSandbox(context, sandboxId);
            }
        }
    }

    /**
     * Runs code inside the sandbox and returns the raw code-run response. Endpoint:
     * {@code POST /toolbox/{sandboxId}/toolbox/process/code-run}.
     */
    private static Map<String, Object> runCode(
        ActionContext context, String sandboxId, String language, String code, Integer timeout) {

        Map<String, Object> body = new LinkedHashMap<>();

        body.put(LANGUAGE, language);
        body.put(CODE, code);

        if (timeout != null) {
            body.put(TIMEOUT, timeout);
        }

        return context
            .http(http -> http.post("/toolbox/" + sandboxId + "/toolbox/process/code-run"))
            .body(Http.Body.of(body))
            .configuration(Http.responseType(Http.ResponseType.JSON))
            .execute()
            .getBody(new TypeReference<Map<String, Object>>() {});
    }

    /**
     * Maps a Daytona code-run response onto the action output. Daytona returns {@code exitCode} and {@code result}
     * (stdout); {@code result} may also be surfaced under {@code artifacts.stdout}, and chart metadata under
     * {@code artifacts.charts}.
     */
    @SuppressWarnings("unchecked")
    static Map<String, Object> toResult(Map<String, Object> response, String sandboxId) {
        int exitCode = 0;

        Object exitCodeValue = response.get("exitCode");

        if (exitCodeValue instanceof Number number) {
            exitCode = number.intValue();
        }

        String stdout = null;
        List<Object> charts = List.of();

        Object result = response.get("result");

        if (result != null) {
            stdout = String.valueOf(result);
        }

        if (response.get("artifacts") instanceof Map<?, ?> artifacts) {
            Map<String, Object> artifactsMap = (Map<String, Object>) artifacts;

            if (stdout == null && artifactsMap.get("stdout") != null) {
                stdout = String.valueOf(artifactsMap.get("stdout"));
            }

            if (artifactsMap.get("charts") instanceof List<?> chartList) {
                charts = List.copyOf(chartList);
            }
        }

        Map<String, Object> output = new LinkedHashMap<>();

        output.put("exitCode", exitCode);
        output.put("stdout", stdout == null ? "" : stdout);
        output.put("success", exitCode == 0);
        output.put(SANDBOX_ID, sandboxId);
        output.put("charts", charts);

        return output;
    }
}
