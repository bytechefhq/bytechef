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

import static com.bytechef.component.ssh.constant.SshConstants.COMMAND;
import static com.bytechef.component.ssh.constant.SshConstants.COMMANDS;
import static com.bytechef.component.ssh.constant.SshConstants.CONTINUE_ON_ERROR;
import static com.bytechef.component.ssh.constant.SshConstants.ERROR;
import static com.bytechef.component.ssh.constant.SshConstants.EXIT_STATUS;
import static com.bytechef.component.ssh.constant.SshConstants.HOST;
import static com.bytechef.component.ssh.constant.SshConstants.HOST_KEY_FINGERPRINT;
import static com.bytechef.component.ssh.constant.SshConstants.PASSWORD;
import static com.bytechef.component.ssh.constant.SshConstants.PORT;
import static com.bytechef.component.ssh.constant.SshConstants.PRIVATE_KEY;
import static com.bytechef.component.ssh.constant.SshConstants.RESULT;
import static com.bytechef.component.ssh.constant.SshConstants.TIMEOUT;
import static com.bytechef.component.ssh.constant.SshConstants.USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ConnectionDefinition;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.exception.ProviderException;
import com.bytechef.component.ssh.connection.SshConnection;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.SecurityUtils;
import net.schmizz.sshj.transport.verification.HostKeyVerifier;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.GenericContainer;

/**
 * @author Igor Beslic
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringJUnitConfig(SshExecuteActionIntTest.TestConfig.class)
public class SshExecuteActionIntTest {

    private static final String SSH_IMAGE =
        "linuxserver/openssh-server@sha256:39ba37d50fdd6be1bf70644c871e5dcb9234ee79ac56424ea03ca08cadf1e7b0";
    private static final int SSH_PORT = 2222;
    private static final String HOME_DIRECTORY = "/config";
    private static final String TEST_DIRECTORY = HOME_DIRECTORY + "/execute-test";
    private static final String TEST_FILE = TEST_DIRECTORY + "/some_file.test";
    private static final String TEST_FILE_CONTENT = "CONTENT";
    private static final String PRIVATE_KEY_PATH = "/tmp/bytechef_test_key";
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(3);

    // I took this from BaseFtpActionIntTest - we have to provide framework for this to avoid duplication
    private static final int PREPARE_MAX_ATTEMPTS = 3;
    private static final long CONTAINER_STARTUP_TIMEOUT_SECONDS = 180;
    private static final long READINESS_TIMEOUT_SECONDS = 90;
    private static final long SEEDING_MARGIN_SECONDS = 60;
    // Worst case: both servers exhaust every prepare attempt, each bounded by container startup plus readiness polling
    private static final long PREPARE_TIMEOUT_SECONDS =
        2 * PREPARE_MAX_ATTEMPTS * (CONTAINER_STARTUP_TIMEOUT_SECONDS + READINESS_TIMEOUT_SECONDS)
            + SEEDING_MARGIN_SECONDS;
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(20);

    private static final Duration READINESS_POLL_INTERVAL = Duration.ofSeconds(1);
    private static final int CONTAINER_LOG_TAIL_LENGTH = 4000;
    static GenericContainer<?> sshContainer;

    protected String sshPassword;
    protected String sshUsername;
    protected String sshHostIp;

    @Value("${bytechef.test.ftp.password:}")
    private String configuredSshPassword;
    @Value("${bytechef.test.ftp.username:}")
    private String configuredSshUsername;
    @Value("${bytechef.test.ftp.host.ip:}")
    private String configuredSshHostIp;

    private static String privateKey;

    @BeforeAll
    @Timeout(value = PREPARE_TIMEOUT_SECONDS, unit = TimeUnit.SECONDS)
    void prepareServer() throws Exception {
        sshPassword = valueOrDefault(configuredSshPassword, getRandomString(12));
        sshUsername = valueOrDefault(configuredSshUsername, "bytechef");

        InetAddress loopbackAddress = InetAddress.getLoopbackAddress();

        sshHostIp = valueOrDefault(configuredSshHostIp, loopbackAddress.getHostAddress());

        StringBuilder diagnostics = new StringBuilder();

        for (int attempt = 1; attempt <= PREPARE_MAX_ATTEMPTS; attempt++) {
            try {
                if (sshContainer == null || !sshContainer.isRunning()) {
                    startSftpContainer();
                }

                execInContainer("mkdir", "-p", TEST_DIRECTORY);
                execInContainer("chown", sshUsername + ":" + sshUsername, TEST_DIRECTORY);

                privateKey = generateAuthorizedKeyPair();

                return;
            } catch (Exception exception) {
                diagnostics.append(System.lineSeparator())
                    .append("attempt ")
                    .append(attempt)
                    .append(": ")
                    .append(exception);

                discardContainer();
            }
        }

        throw new IllegalStateException(
            "FTP server could not be prepared after " + PREPARE_MAX_ATTEMPTS + " attempts:" + diagnostics);
    }

    private void startSftpContainer() {
        GenericContainer<?> container = new GenericContainer<>(SSH_IMAGE)
            .withEnv("PASSWORD_ACCESS", "true")
            .withEnv("USER_NAME", sshUsername)
            .withEnv("USER_PASSWORD", sshPassword)
            .withExposedPorts(SSH_PORT)
            .withStartupTimeout(STARTUP_TIMEOUT);

        container.start();

        sshContainer = container;

        awaitSshReady();
    }

    private void awaitSshReady() {
        Duration readinessTimeout = Duration.ofSeconds(READINESS_TIMEOUT_SECONDS);
        long deadline = System.nanoTime() + readinessTimeout.toNanos();
        Exception lastFailure = null;

        while (System.nanoTime() < deadline) {
            SSHClient sshClient = null;

            try {
                sshClient = new SSHClient();

                sshClient.addHostKeyVerifier(new PromiscuousVerifier());
                sshClient.setConnectTimeout((int) PROBE_TIMEOUT.toMillis());
                sshClient.setTimeout((int) PROBE_TIMEOUT.toMillis());

                sshClient.connect(sshHostIp, sshContainer.getMappedPort(SSH_PORT));
                sshClient.authPassword(sshUsername, sshPassword);

                return;
            } catch (Exception exception) {
                lastFailure = exception;

                sleep(READINESS_POLL_INTERVAL);
            } finally {
                disconnectQuietly(sshClient);
            }
        }

        throw new IllegalStateException(
            "SSH server did not become ready within " + readinessTimeout + describeContainerLogs(sshContainer),
            lastFailure);
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private static void disconnectQuietly(SSHClient sshClient) {
        if (sshClient != null && sshClient.isConnected()) {
            try {
                sshClient.disconnect();
            } catch (IOException ioException) {
                // Intentionally ignored - best-effort cleanup of a probe connection
            }
        }
    }

    private static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread()
                .interrupt();

            throw new IllegalStateException("Interrupted while waiting for a test container", interruptedException);
        }
    }

    private static String describeContainerLogs(GenericContainer<?> container) {
        if (container == null) {
            return "; no container to read logs from";
        }

        try {
            String logs = container.getLogs();

            if (logs.length() > CONTAINER_LOG_TAIL_LENGTH) {
                logs = logs.substring(logs.length() - CONTAINER_LOG_TAIL_LENGTH);
            }

            return "; container logs:" + System.lineSeparator() + logs;
        } catch (RuntimeException runtimeException) {
            return "; container logs unavailable: " + runtimeException;
        }
    }

    private void discardContainer() {
        if (sshContainer != null) {
            sshContainer.stop();

            sshContainer = null;
        }
    }

    @Test
    void testExecuteListsDirectory() {
        List<Map<String, Object>> results = perform(
            passwordConnectionParameters(),
            Map.of(COMMANDS, List.of("ls -la " + TEST_DIRECTORY)));

        assertEquals(1, results.size());

        Map<String, Object> result = results.getFirst();

        assertEquals(0, result.get(EXIT_STATUS));
        assertEquals("ls -la " + TEST_DIRECTORY, result.get(COMMAND));

        String output = (String) result.get(RESULT);

        assertTrue(output.contains("total"), "Expected a directory listing, got: " + output);
        assertTrue(output.contains("."), "Expected a directory listing, got: " + output);
        assertEquals("", result.get(ERROR));
    }

    @Test
    void testExecuteFileOperations() {
        List<Map<String, Object>> results = perform(
            passwordConnectionParameters(),
            Map.of(
                COMMANDS,
                List.of(
                    "touch " + TEST_FILE,
                    "echo \"" + TEST_FILE_CONTENT + "\" > " + TEST_FILE,
                    "grep CON " + TEST_FILE,
                    "rm " + TEST_FILE,
                    "ls " + TEST_FILE)));

        assertEquals(5, results.size());

        assertEquals(0, results.get(0)
            .get(EXIT_STATUS));
        assertEquals(0, results.get(1)
            .get(EXIT_STATUS));
        assertEquals(TEST_FILE_CONTENT, results.get(2)
            .get(RESULT));
        assertEquals(0, results.get(3)
            .get(EXIT_STATUS));

        Map<String, Object> listRemovedFileResult = results.get(4);

        assertNotEquals(0, listRemovedFileResult.get(EXIT_STATUS), "Listing a removed file should fail");
        assertFalse(
            ((String) listRemovedFileResult.get(ERROR)).isEmpty(), "Listing a removed file should report an error");
    }

    @Test
    void testExecuteChecksDiskSpace() {
        List<Map<String, Object>> results = perform(
            passwordConnectionParameters(), Map.of(COMMANDS, List.of("df -h")));

        Map<String, Object> result = results.getFirst();

        assertEquals(0, result.get(EXIT_STATUS));

        String output = (String) result.get(RESULT);

        assertTrue(output.contains("Filesystem"), "Expected disk usage output, got: " + output);
        assertTrue(output.contains("Use%"), "Expected disk usage output, got: " + output);
    }

    @Test
    void testExecuteSkipsEmptyCommands() {

        List<String> commands = new ArrayList<>();

        commands.add("");
        commands.add("df -h");
        commands.add(" ");
        commands.add("   ");
        commands.add(null);
        commands.add("ls -la");

        List<Map<String, Object>> results = perform(
            passwordConnectionParameters(), Map.of(COMMANDS, commands));

        assertEquals(2, results.size());

        Map<String, Object> result = results.getFirst();

        assertEquals(0, result.get(EXIT_STATUS));

        String output = (String) result.get(RESULT);

        assertTrue(output.contains("Filesystem"), "Expected disk usage output, got: " + output);
        assertTrue(output.contains("Use%"), "Expected disk usage output, got: " + output);

        result = results.getLast();

        output = (String) result.get(RESULT);
        assertTrue(output.contains("total"), "Expected list directory output, got: " + output);
    }

    @Test
    void testExecuteStopsOnFirstFailedCommand() {
        List<Map<String, Object>> results = perform(
            passwordConnectionParameters(),
            Map.of(COMMANDS, List.of("echo first", "cat /no/such/file", "echo third")));

        assertEquals(2, results.size(), "Execution should stop at the failed command");
        assertEquals("first", results.getFirst()
            .get(RESULT));

        Map<String, Object> failedResult = results.get(1);

        assertNotEquals(0, failedResult.get(EXIT_STATUS));
        assertTrue(
            ((String) failedResult.get(ERROR)).contains("/no/such/file"),
            "Expected the standard error output of the failed command");
    }

    @Test
    void testExecuteContinuesOnError() {
        List<Map<String, Object>> results = perform(
            passwordConnectionParameters(),
            Map.of(
                COMMANDS, List.of("echo first", "cat /no/such/file", "echo third"),
                CONTINUE_ON_ERROR, true));

        assertEquals(3, results.size(), "Every command should run when errors are ignored");
        assertEquals("third", results.get(2)
            .get(RESULT));
        assertEquals(0, results.get(2)
            .get(EXIT_STATUS));
    }

    @Test
    void testExecuteRunsEveryCommandInItsOwnSession() {
        List<Map<String, Object>> results = perform(
            passwordConnectionParameters(), Map.of(COMMANDS, List.of("cd /tmp", "pwd", "cd /tmp && pwd")));

        assertEquals(HOME_DIRECTORY, results.get(1)
            .get(RESULT), "A directory change must not leak into the next command");
        assertEquals("/tmp", results.get(2)
            .get(RESULT));
    }

    @Test
    void testExecuteWithPrivateKey() {
        Map<String, Object> connectionParameters = new HashMap<>(baseConnectionParameters());

        connectionParameters.put(PRIVATE_KEY, privateKey);

        List<Map<String, Object>> results = perform(
            MockParametersFactory.create(connectionParameters), Map.of(COMMANDS, List.of("whoami")));

        assertEquals(sshUsername, results.getFirst()
            .get(RESULT));
    }

    @Test
    void testExecuteWithMatchingHostKeyFingerprint() throws IOException {
        Map<String, Object> connectionParameters = new HashMap<>(baseConnectionParameters());

        connectionParameters.put(PASSWORD, sshPassword);
        connectionParameters.put(HOST_KEY_FINGERPRINT, readHostKeyFingerprint());

        List<Map<String, Object>> results = perform(
            MockParametersFactory.create(connectionParameters), Map.of(COMMANDS, List.of("echo verified")));

        assertEquals("verified", results.getFirst()
            .get(RESULT));
    }

    @Test
    void testExecuteWithMismatchedHostKeyFingerprintFails() {
        Map<String, Object> connectionParameters = new HashMap<>(baseConnectionParameters());

        connectionParameters.put(PASSWORD, sshPassword);
        connectionParameters.put(
            HOST_KEY_FINGERPRINT, "SHA256:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

        assertThrows(
            ProviderException.class,
            () -> perform(MockParametersFactory.create(connectionParameters), Map.of(COMMANDS, List.of("echo hello"))));
    }

    @Test
    void testConnectionTestSucceeds() {
        ConnectionDefinition.TestConsumer testConsumer = connectionTestConsumer();

        testConsumer.accept(passwordConnectionParameters(), mock(ActionContext.class));
    }

    @Test
    void testConnectionTestFailsWithWrongPassword() {
        Map<String, Object> connectionParameters = new HashMap<>(baseConnectionParameters());

        connectionParameters.put(PASSWORD, getRandomString(15));

        ConnectionDefinition.TestConsumer testConsumer = connectionTestConsumer();

        assertThrows(
            ProviderException.class,
            () -> testConsumer.accept(MockParametersFactory.create(connectionParameters), mock(ActionContext.class)));
    }

    @Test
    void testExecuteWithWrongPasswordFails() {
        Map<String, Object> connectionParameters = new HashMap<>(baseConnectionParameters());

        connectionParameters.put(PASSWORD, getRandomString(20));

        assertThrows(
            ProviderException.class,
            () -> perform(MockParametersFactory.create(connectionParameters), Map.of(COMMANDS, List.of("echo hello"))));
    }

    @Test
    void testExecuteFailsWhenCommandOutputStaysOpen() {
        // The backgrounded command inherits the output stream and keeps it open after the command itself finished
        assertThrows(
            ProviderException.class,
            () -> perform(
                passwordConnectionParameters(), Map.of(COMMANDS, List.of("(sleep 10 &) ; echo started"), TIMEOUT, 1)));
    }

    @Test
    void testExecuteTimesOut() {
        assertThrows(
            ProviderException.class,
            () -> perform(
                passwordConnectionParameters(), Map.of(COMMANDS, List.of("sleep 10"), TIMEOUT, 1)));
    }

    private static ConnectionDefinition.TestConsumer connectionTestConsumer() {
        Optional<ConnectionDefinition.TestConsumer> testConsumer = SshConnection.CONNECTION_DEFINITION.getTest();

        return testConsumer.orElseThrow(
            () -> new IllegalStateException("The SSH connection has to define a connection test"));
    }

    private static List<Map<String, Object>> perform(
        Parameters connectionParameters, Map<String, Object> inputParameters) {

        return SshExecuteAction.perform(
            MockParametersFactory.create(inputParameters), connectionParameters, mock(ActionContext.class));
    }

    private Map<String, Object> baseConnectionParameters() {
        Map<String, Object> connectionParameters = new HashMap<>();

        connectionParameters.put(HOST, sshContainer.getHost());
        connectionParameters.put(PORT, sshContainer.getMappedPort(SSH_PORT));
        connectionParameters.put(USERNAME, sshUsername);

        return connectionParameters;
    }

    private Parameters passwordConnectionParameters() {
        Map<String, Object> connectionParameters = new HashMap<>(baseConnectionParameters());

        connectionParameters.put(PASSWORD, sshPassword);

        return MockParametersFactory.create(connectionParameters);
    }

    /**
     * Generates a key pair inside the container, authorizes the public key for the test user and returns the private
     * key in the OpenSSH format a ByteChef user would paste into the connection.
     */
    private String generateAuthorizedKeyPair() throws Exception {
        execInContainer("ssh-keygen", "-t", "ed25519", "-N", "", "-f", PRIVATE_KEY_PATH);
        execInContainer(
            "sh", "-c",
            "mkdir -p " + HOME_DIRECTORY + "/.ssh" +
                " && cat " + PRIVATE_KEY_PATH + ".pub >> " + HOME_DIRECTORY + "/.ssh/authorized_keys" +
                " && chown -R " + sshUsername + ":" + sshUsername + " " + HOME_DIRECTORY + "/.ssh" +
                " && chmod 700 " + HOME_DIRECTORY + "/.ssh" +
                " && chmod 600 " + HOME_DIRECTORY + "/.ssh/authorized_keys");

        return execInContainer("cat", PRIVATE_KEY_PATH);
    }

    /**
     * Reads the fingerprint of the host key the server actually presents, so that the verified fingerprint matches the
     * negotiated host key algorithm.
     */
    private static String readHostKeyFingerprint() throws IOException {
        StringBuilder fingerprint = new StringBuilder();

        try (SSHClient sshClient = new SSHClient()) {
            sshClient.addHostKeyVerifier(new HostKeyVerifier() {

                @Override
                public boolean verify(String hostname, int port, PublicKey key) {
                    fingerprint.append(SecurityUtils.getFingerprint(key));

                    return true;
                }

                @Override
                public List<String> findExistingAlgorithms(String hostname, int port) {
                    return List.of();
                }
            });

            sshClient.connect(sshContainer.getHost(), sshContainer.getMappedPort(SSH_PORT));
        }

        return fingerprint.toString();
    }

    private static String execInContainer(String... command) throws Exception {
        org.testcontainers.containers.Container.ExecResult execResult = sshContainer.execInContainer(command);

        if (execResult.getExitCode() != 0) {
            throw new IllegalStateException(
                "Command " + String.join(" ", command) + " failed: " + execResult.getStderr());
        }

        return execResult.getStdout();
    }

    private static String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
    }

    private static String getRandomString(int length) {
        byte[] bytes = RandomUtils.secure()
            .randomBytes(length * 2);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    @Configuration
    static class TestConfig {
    }
}
