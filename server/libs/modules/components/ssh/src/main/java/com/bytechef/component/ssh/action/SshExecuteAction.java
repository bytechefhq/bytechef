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

package com.bytechef.component.ssh.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.array;
import static com.bytechef.component.definition.ComponentDsl.bool;
import static com.bytechef.component.definition.ComponentDsl.integer;
import static com.bytechef.component.definition.ComponentDsl.object;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.ssh.constant.SshConstants.COMMAND;
import static com.bytechef.component.ssh.constant.SshConstants.COMMANDS;
import static com.bytechef.component.ssh.constant.SshConstants.CONTINUE_ON_ERROR;
import static com.bytechef.component.ssh.constant.SshConstants.ERROR;
import static com.bytechef.component.ssh.constant.SshConstants.EXIT_STATUS;
import static com.bytechef.component.ssh.constant.SshConstants.RESULT;
import static com.bytechef.component.ssh.constant.SshConstants.TIMEOUT;

import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.ssh.util.SshClientUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.ConnectionException;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Igor Beslic
 */
public class SshExecuteAction {

    private static final int DEFAULT_TIMEOUT_SECONDS = 60;

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("execute")
        .title("Execute Shell Commands")
        .description(
            "Executes shell commands on a remote host over SSH and returns the output of every command. Each " +
                "command runs in its own session, so a working directory or an environment variable set by one " +
                "command is not visible to the next one. Chain such commands instead, for example " +
                "'cd /var/log && ls -la'.")
        .properties(
            array(COMMANDS)
                .label("Commands")
                .description(
                    "The shell commands to execute on the remote host, in the order they are listed in the array.")
                .items(string())
                .placeholder("shell command expression")
                .required(true),
            bool(CONTINUE_ON_ERROR)
                .label("Continue On Error")
                .description(
                    "Keep executing the remaining commands when a command exits with a non-zero exit status. When " +
                        "disabled, execution stops at the first failed command and the output collected so far is " +
                        "returned.")
                .defaultValue(false)
                .required(false),
            integer(TIMEOUT)
                .label("Timeout")
                .description("The number of seconds to wait for a single command to finish.")
                .defaultValue(DEFAULT_TIMEOUT_SECONDS)
                .minValue(1)
                .required(false))
        .output(
            outputSchema(
                array()
                    .items(
                        object()
                            .properties(
                                string(COMMAND).description("The executed shell command."),
                                string(RESULT).description("The standard output of the command."),
                                string(ERROR).description("The standard error output of the command."),
                                integer(EXIT_STATUS).description("The exit status of the command, 0 on success.")))))
        .perform(SshExecuteAction::perform);

    private SshExecuteAction() {
    }

    protected static List<Map<String, Object>> perform(
        Parameters inputParameters, Parameters connectionParameters, Context context) {

        List<Map<String, Object>> results = new ArrayList<>();

        try (SSHClient sshClient = SshClientUtils.connect(connectionParameters)) {
            List<String> commands = inputParameters.getRequiredList(COMMANDS, String.class);
            int timeout = inputParameters.getInteger(TIMEOUT, DEFAULT_TIMEOUT_SECONDS);

            for (String command : commands) {
                if (StringUtils.isBlank(command)) {
                    continue;
                }

                Map<String, Object> result = executeCommand(sshClient, command, timeout);

                results.add(result);

                Integer exitStatus = (Integer) result.get(EXIT_STATUS);

                if (((exitStatus == null) || (exitStatus != 0)) &&
                    !inputParameters.getBoolean(CONTINUE_ON_ERROR, false)) {

                    break;
                }
            }

            return results;
        } catch (IOException ioException) {
            throw new ProviderException("Failed to execute commands: " + ioException.getMessage(), ioException);
        }
    }

    private static Map<String, Object> executeCommand(SSHClient sshClient, String command, int timeout)
        throws IOException {

        try (Session session = sshClient.startSession()) {
            Session.Command remoteCommand = session.exec(command);

            // Both streams are drained while the command runs. Reading them one after the other would deadlock as
            // soon as the channel window of the stream that is not being read fills up.
            OutputReader outputReader = OutputReader.start(remoteCommand.getInputStream());
            OutputReader errorOutputReader = OutputReader.start(remoteCommand.getErrorStream());

            try {
                remoteCommand.join(timeout, TimeUnit.SECONDS);
            } catch (ConnectionException connectionException) {
                throw new ProviderException(
                    "Command '" + command + "' did not finish within " + timeout + " seconds", connectionException);
            }

            Map<String, Object> result = new HashMap<>();

            result.put(COMMAND, command);
            result.put(RESULT, outputReader.awaitOutput(command, timeout));
            result.put(ERROR, errorOutputReader.awaitOutput(command, timeout));
            result.put(EXIT_STATUS, remoteCommand.getExitStatus());

            return result;
        }
    }

    /**
     * Reads a command output stream on a virtual thread, so that standard output and standard error can be consumed at
     * the same time.
     */
    private static class OutputReader {

        private final AtomicReference<String> outputReference = new AtomicReference<>("");
        private final AtomicReference<IOException> failureReference = new AtomicReference<>();
        private final Thread thread;

        private OutputReader(InputStream inputStream) {
            thread = Thread.ofVirtual()
                .start(() -> {
                    try {
                        String output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

                        outputReference.set(output.stripTrailing());
                    } catch (IOException ioException) {
                        failureReference.set(ioException);
                    }
                });
        }

        static OutputReader start(InputStream inputStream) {
            return new OutputReader(inputStream);
        }

        String awaitOutput(String command, int timeout) throws IOException {
            try {
                // A command can hand its output stream over to a process that outlives it, in which case the stream
                // stays open after the command itself finished.
                if (!thread.join(Duration.ofSeconds(timeout))) {
                    throw new ProviderException(
                        "Command '" + command + "' did not close its output within " + timeout + " seconds");
                }
            } catch (InterruptedException interruptedException) {
                Thread currentThread = Thread.currentThread();

                currentThread.interrupt();

                throw new ProviderException(
                    "Interrupted while reading the output of command '" + command + "'", interruptedException);
            }

            IOException failure = failureReference.get();

            if (failure != null) {
                throw failure;
            }

            return outputReference.get();
        }
    }
}
