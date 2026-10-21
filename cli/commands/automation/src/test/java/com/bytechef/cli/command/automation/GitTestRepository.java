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

package com.bytechef.cli.command.automation;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Creates a throwaway git repository with its own {@code .gitignore}. A repository's {@code .gitignore} outranks the
 * user's global excludes file, so a test can pin a file as ignored or not ignored whatever the machine's git setup.
 *
 * @author Ivica Cardic
 */
final class GitTestRepository {

    private static final long GIT_TIMEOUT_SECONDS = 30;

    private GitTestRepository() {
    }

    @SuppressFBWarnings(
        value = "COMMAND_INJECTION",
        justification = "Runs the fixed git executable on a test-owned temporary directory.")
    static Path create(Path directory, String gitignoreContent) throws IOException, InterruptedException {
        Files.createDirectories(directory);

        ProcessBuilder processBuilder = new ProcessBuilder("git", "init", "-q", directory.toString());

        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process process = processBuilder.start();

        if (!process.waitFor(GIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly();

            throw new IllegalStateException("git init did not finish in " + GIT_TIMEOUT_SECONDS + " seconds");
        }

        if (process.exitValue() != 0) {
            throw new IllegalStateException("git init failed with exit code " + process.exitValue());
        }

        Files.writeString(directory.resolve(".gitignore"), gitignoreContent);

        return directory;
    }
}
