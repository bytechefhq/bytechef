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

package com.bytechef.platform.component.runner;

/**
 * @author Ivica Cardic
 */
public class TaskRunnerConstants {

    public static final String TASK_RUNNER = "taskRunner";
    public static final String TYPE = "type";

    public static final String DOCKER = "docker";
    public static final String GRAALVM = "graalvm";
    public static final String PROCESS = "process";

    public static final String MODE = "mode";
    public static final String STRICT = "strict";
    public static final String TRUSTED = "trusted";

    public static final String INHERIT_ENVIRONMENT = "inheritEnvironment";
    public static final String INTERPRETER = "interpreter";

    public static final String CPU = "cpu";
    public static final String ENTRYPOINT = "entrypoint";
    public static final String EXTRA_HOSTS = "extraHosts";
    public static final String IMAGE = "image";
    public static final String MEMORY = "memory";
    public static final String NETWORK_MODE = "networkMode";
    public static final String PULL_POLICY = "pullPolicy";
    public static final String USER = "user";

    public static final String CREDENTIALS = "credentials";
    public static final String DELETE = "delete";
    public static final String HOST = "host";
    public static final String KILL_GRACE_PERIOD = "killGracePeriod";
    public static final String PASSWORD = "password";
    public static final String PRIVILEGED = "privileged";
    public static final String REGISTRY = "registry";
    public static final String REGISTRY_TOKEN = "registryToken";
    public static final String RESUME = "resume";
    public static final String USERNAME = "username";
    public static final String VOLUMES = "volumes";
    public static final String WAIT_UNTIL_COMPLETION = "waitUntilCompletion";
    public static final String WAIT_UNTIL_RUNNING = "waitUntilRunning";

    public static final String ENV = "env";
    public static final String INPUT_FILES = "inputFiles";
    public static final String OUTPUT_FILES = "outputFiles";
    public static final String TIMEOUT = "timeout";

    public static final String RESULT_EXIT_CODE = "exitCode";
    public static final String RESULT_STDOUT = "stdout";
    public static final String RESULT_STDERR = "stderr";
    public static final String RESULT_VARS = "vars";

    private TaskRunnerConstants() {
    }
}
