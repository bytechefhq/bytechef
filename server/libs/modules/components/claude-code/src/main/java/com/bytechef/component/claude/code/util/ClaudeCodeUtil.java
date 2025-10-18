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

package com.bytechef.component.claude.code.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.concurrent.TimeoutException;
import org.zeroturnaround.exec.ProcessExecutor;

/**
 * Utility class for executing Claude Code CLI commands within workflow automation.
 *
 * @author Marko Kriskovic
 */
public class ClaudeCodeUtil {

    private ClaudeCodeUtil() {
    }

    /**
     * Executes the provided bash command by writing it to a temporary script file and running it.
     *
     * <p>
     * <b>Security Note:</b> Command injection is intentional for this component. The Claude Code component integrates
     * the Claude Code CLI into workflows, allowing AI-assisted code generation and execution. Access to this component
     * should be restricted through workflow-level permissions. Commands are constructed from workflow configuration,
     * not from untrusted user input.
     */
    @SuppressFBWarnings("COMMAND_INJECTION")
    public static String execute(String bashCommand) throws IOException, InterruptedException, TimeoutException {
        File scriptFile = File.createTempFile("_script", ".sh");

        writeStringToFile(scriptFile, bashCommand);

        try {
            Runtime runtime = Runtime.getRuntime();

            Process chmodProcess = runtime.exec(new String[] {
                "chmod", "u+x", scriptFile.getAbsolutePath()
            });

            int chmodRetCode = chmodProcess.waitFor();

            if (chmodRetCode != 0) {
                throw new IllegalStateException("Failed to chmod %s".formatted(chmodRetCode));
            }

            return new ProcessExecutor()
                .command(scriptFile.getAbsolutePath())
                .readOutput(true)
                .execute()
                .outputUTF8();
        } finally {
            deleteRecursively(scriptFile.toPath());
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
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

    }

    private static void writeStringToFile(File file, String string) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            writer.write(string);
        }
    }
}
