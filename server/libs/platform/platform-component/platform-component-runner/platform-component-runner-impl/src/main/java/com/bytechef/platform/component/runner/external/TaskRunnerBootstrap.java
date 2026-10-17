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

package com.bytechef.platform.component.runner.external;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Optional;

/**
 * @author Ivica Cardic
 */
public final class TaskRunnerBootstrap {

    public static final String CONNECTIONS_FILE_VARIABLE = "BYTECHEF_CONNECTIONS_FILE";

    private TaskRunnerBootstrap() {
    }

    public static String append(String languageId, String source, String runnerType) {
        String separated = source.endsWith("\n") ? source : source + "\n";

        return switch (ExternalLanguage.of(languageId)) {
            case JAVASCRIPT -> separated + javaScriptBootstrap(runnerType);
            case PYTHON -> separated + pythonBootstrap(runnerType);
            case BASH, SHELL -> separated;
        };
    }

    public static boolean isSupported(String languageId) {
        Optional<ExternalLanguage> externalLanguage = ExternalLanguage.find(languageId);

        return externalLanguage.isPresent();
    }

    public static String sourceFileName(String languageId) {
        return switch (ExternalLanguage.of(languageId)) {
            case JAVASCRIPT -> "script.js";
            case PYTHON -> "script.py";
            case BASH -> "script.sh";
            case SHELL -> "commands.sh";
        };
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static String javaScriptBootstrap(String runnerType) {
        return """

            /* ByteChef bootstrap - appended, so the lines above keep their numbers. */
            (async function bytechefMain() {
                const bytechefFs = require('fs');
                const bytechefPath = require('path');
                const bytechefInput = JSON.parse(
                    bytechefFs.readFileSync(process.env.BYTECHEF_INPUT_FILE, 'utf8'));
                const bytechefContext = new Proxy({}, {
                    get(target, property) {
                        throw new Error(
                            'context.' + String(property) + ' is not available under the %s runner');
                    }
                });
                const bytechefResult = await perform(bytechefInput, bytechefContext);

                bytechefFs.writeFileSync(
                    bytechefPath.join(process.env.BYTECHEF_OUTPUT_DIR, 'output.json'),
                    JSON.stringify(bytechefResult === undefined ? null : bytechefResult));
            })().catch(function (error) {
                console.error(error && error.stack ? error.stack : String(error));
                process.exit(1);
            });
            """.formatted(runnerType);
    }

    @SuppressFBWarnings("VA_FORMAT_STRING_USES_NEWLINE")
    private static String pythonBootstrap(String runnerType) {
        return """

            # ByteChef bootstrap - appended, so the lines above keep their numbers.
            def _bytechef_main():
                import json
                import os

                class _BytechefContext:
                    def __getattr__(self, name):
                        raise RuntimeError(
                            "context." + name + " is not available under the %s runner")

                with open(os.environ["BYTECHEF_INPUT_FILE"], "r") as bytechef_input_file:
                    bytechef_input = json.load(bytechef_input_file)

                bytechef_result = perform(bytechef_input, _BytechefContext())

                bytechef_output_path = os.path.join(os.environ["BYTECHEF_OUTPUT_DIR"], "output.json")

                with open(bytechef_output_path, "w") as bytechef_output_file:
                    json.dump(bytechef_result, bytechef_output_file)


            _bytechef_main()
            """.formatted(runnerType);
    }
}
