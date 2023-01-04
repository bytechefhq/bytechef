
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
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;

import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import com.bytechef.hermes.component.exception.ActionExecutionException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.zeroturnaround.exec.ProcessExecutor;

/**
 * The Bash component executes arbitrary Bash scripts.
 *
 * @author Ivica Cardic
 */
public class BashComponentHandler implements ComponentHandler {

    private ComponentDefinition componentDefinition = component(BASH)
        .display(display("Bash").description("Allows you to run arbitrary Bash scripts."))
        .actions(action(EXECUTE)
            .display(display("Execute").description("Executes the script."))
            .properties(string(SCRIPT)
                .label("Script")
                .description("Script written in bash.")
                .required(true))
            .output(string())
            .perform(this::performExecute));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    protected String performExecute(Context context, ExecutionParameters executionParameters) {
        try {
            File scriptFile = File.createTempFile("_script", ".sh");

            writeStringToFile(scriptFile, executionParameters.getRequiredString(SCRIPT));

            try {
                Runtime runtime = Runtime.getRuntime();

                Process chmodProcess = runtime.exec(String.format("chmod u+x %s", scriptFile.getAbsolutePath()));

                int chmodRetCode = chmodProcess.waitFor();

                if (chmodRetCode != 0) {
                    throw new ActionExecutionException("Failed to chmod %s".formatted(chmodRetCode));
                }

                return new ProcessExecutor().command(scriptFile.getAbsolutePath())
                    .readOutput(true)
                    .execute()
                    .outputUTF8();
            } finally {
                deleteRecursively(scriptFile.toPath());
            }
        } catch (Exception exception) {
            throw new ActionExecutionException("Unable to handle task " + executionParameters, exception);
        }
    }

    private boolean deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return false;
        }

        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);

                return FileVisitResult.CONTINUE;
            }
        });

        return true;
    }

    private void writeStringToFile(File file, String str) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            writer.write(str);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
