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

package com.bytechef.component.bash;

import static com.bytechef.component.bash.constants.BashConstants.BASH;
import static com.bytechef.component.bash.constants.BashConstants.EXECUTE;
import static com.bytechef.component.bash.constants.BashConstants.SCRIPT;
import static com.bytechef.hermes.component.ComponentDSL.action;
import static com.bytechef.hermes.component.ComponentDSL.createComponent;
import static com.bytechef.hermes.component.ComponentDSL.display;
import static com.bytechef.hermes.component.ComponentDSL.string;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Bash component executes arbitrary Bash scripts.
 *
 * @author Ivica Cardic
 */
public class BashComponentHandler implements ComponentHandler {

    private static final Logger logger = LoggerFactory.getLogger(BashComponentHandler.class);

    private ComponentDefinition componentDefinition = createComponent(BASH)
            .display(display("Bash").description("Allows you to run arbitrary Bash scripts."))
            .actions(action(EXECUTE)
                    .display(display("Execute").description("Executes the script."))
                    .properties(string(SCRIPT)
                            .label("Script")
                            .description("Script written in bash.")
                            .required(true))
                    .output(string())
                    .performFunction(this::performExecute));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected String performExecute(Context context, ExecutionParameters executionParameters) {
        try {
            File scriptFile = File.createTempFile("_script", ".sh");
            File logFile = File.createTempFile("log", null);

            FileUtils.writeStringToFile(
                    scriptFile, executionParameters.getRequiredString("script"), StandardCharsets.UTF_8);

            try (PrintStream stream = new PrintStream(logFile, StandardCharsets.UTF_8)) {
                Runtime runtime = Runtime.getRuntime();
                Process chmodProcess = runtime.exec(String.format("chmod u+x %s", scriptFile.getAbsolutePath()));
                int chmodRetCode = chmodProcess.waitFor();

                if (chmodRetCode != 0) {
                    throw new ExecuteException("Failed to chmod", chmodRetCode);
                }

                CommandLine commandLine = new CommandLine(scriptFile.getAbsolutePath());

                logger.debug("{}", commandLine);

                DefaultExecutor executor = new DefaultExecutor();

                executor.setStreamHandler(new PumpStreamHandler(stream));
                executor.execute(commandLine);

                return FileUtils.readFileToString(logFile, StandardCharsets.UTF_8);
            } catch (ExecuteException e) {
                throw new ExecuteException(
                        e.getMessage(),
                        e.getExitValue(),
                        new RuntimeException(FileUtils.readFileToString(logFile, StandardCharsets.UTF_8)));
            } finally {
                FileUtils.deleteQuietly(logFile);
                FileUtils.deleteQuietly(scriptFile);
            }
        } catch (Exception exception) {
            throw new ActionExecutionException("Unable to handle task " + executionParameters, exception);
        }
    }
}
