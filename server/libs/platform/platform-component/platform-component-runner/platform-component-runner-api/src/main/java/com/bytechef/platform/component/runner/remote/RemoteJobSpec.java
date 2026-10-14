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

package com.bytechef.platform.component.runner.remote;

import com.bytechef.component.definition.Parameters;
import java.util.List;
import java.util.Map;

/**
 * Everything a backend needs to start one job. Remote paths are derived from the backend's working directory so every
 * runner shares the same layout underneath it.
 */
public record RemoteJobSpec(
    RemoteJobKey key, String languageId, String sourceFileName, List<String> interpreter,
    Map<String, String> environment, String remoteWorkingDirectory, RemotePullPolicy pullPolicy,
    Parameters runnerParameters) {

    public static final String INPUT_FILE_NAME = "input.json";
    public static final String OUTPUT_DIRECTORY_NAME = "output";
    public static final String STDERR_FILE_NAME = ".stderr";

    public RemoteJobSpec {
        if (!remoteWorkingDirectory.startsWith("/")) {
            throw new IllegalArgumentException("remoteWorkingDirectory must be absolute");
        }

        remoteWorkingDirectory = remoteWorkingDirectory.length() > 1 && remoteWorkingDirectory.endsWith("/")
            ? remoteWorkingDirectory.substring(0, remoteWorkingDirectory.length() - 1)
            : remoteWorkingDirectory;
        interpreter = List.copyOf(interpreter);
        environment = Map.copyOf(environment);
    }

    public String remoteInputFilePath() {
        return remoteWorkingDirectory + "/" + INPUT_FILE_NAME;
    }

    public String remoteOutputDirectory() {
        return remoteWorkingDirectory + "/" + OUTPUT_DIRECTORY_NAME;
    }

    public String remoteSourcePath() {
        return remoteWorkingDirectory + "/" + sourceFileName;
    }

    public String remoteStderrPath() {
        return remoteWorkingDirectory + "/" + STDERR_FILE_NAME;
    }
}
