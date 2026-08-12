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

package com.bytechef.component.ftp.action;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

/**
 * @author Igor Beslic
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringJUnitConfig(BaseFtpActionIntTest.TestConfig.class)
public class BaseFtpActionIntTest {

    protected static final List<String> SEEDED_FILE_SUFFIXES = List.of("for-delete", "001", "002", "003", "004", "005");
    protected static final int SEEDED_FILE_COUNT = SEEDED_FILE_SUFFIXES.size() + 2;
    protected static final String TEST_CONTENT = "Hello from FTP Integration Test!";
    protected static final String TEST_FILE_BASE_NAME = "test-document";
    protected static final String TEST_FILE_BASE_EXTENSION = ".txt";

    protected static final String TEST_FILE_NAME = TEST_FILE_BASE_NAME + TEST_FILE_BASE_EXTENSION;

    protected static final String FTP_REMOTE_PATH = TEST_FILE_NAME;
    protected static final String SFTP_UPLOAD_DIR = "upload";

    protected static final String SFTP_REMOTE_PATH = "/" + SFTP_UPLOAD_DIR + "/" + TEST_FILE_NAME;
    protected static final String LARGE_TEST_FILE_NAME = "large-" + TEST_FILE_NAME;
    protected static final String LARGE_SFTP_REMOTE_PATH = "/" + SFTP_UPLOAD_DIR + "/" + LARGE_TEST_FILE_NAME;
    protected static final String LARGE_TEST_CONTENT_LINE =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_";
    protected static final int LARGE_TEST_CONTENT_LINE_COUNT = 64;
    protected static final String LARGE_TEST_CONTENT =
        (LARGE_TEST_CONTENT_LINE + "\n").repeat(LARGE_TEST_CONTENT_LINE_COUNT);

    private static final String FTP_IMAGE =
        "delfer/alpine-ftp-server@sha256:60bb774d8408d9d4d5c74d05d1c086a34ce192c6c1a142ffac268cac0dbc6fac";
    private static final String SFTP_IMAGE =
        "atmoz/sftp@sha256:0960390462a4441dbb63698d7c185b76a41ffcee7b78ff4adf275f3e66f9c475";
    private static final int FTP_CONTROL_PORT = 21;
    private static final int SFTP_PORT = 22;
    private static final int PASSIVE_PORT_COUNT = 10;
    private static final int PASSIVE_PORT_SEARCH_FIRST = 30100;
    private static final int PASSIVE_PORT_SEARCH_LAST = 32700;
    private static final int PREPARE_MAX_ATTEMPTS = 3;
    private static final long CONTAINER_STARTUP_TIMEOUT_SECONDS = 180;
    private static final long READINESS_TIMEOUT_SECONDS = 90;
    private static final long SEEDING_MARGIN_SECONDS = 60;
    // Worst case: both servers exhaust every prepare attempt, each bounded by container startup plus readiness polling
    private static final long PREPARE_TIMEOUT_SECONDS =
        2 * PREPARE_MAX_ATTEMPTS * (CONTAINER_STARTUP_TIMEOUT_SECONDS + READINESS_TIMEOUT_SECONDS)
            + SEEDING_MARGIN_SECONDS;
    private static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofSeconds(CONTAINER_STARTUP_TIMEOUT_SECONDS);
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration READINESS_TIMEOUT = Duration.ofSeconds(READINESS_TIMEOUT_SECONDS);
    private static final Duration READINESS_POLL_INTERVAL = Duration.ofSeconds(1);
    private static final int CONTAINER_LOG_TAIL_LENGTH = 4000;

    static FixedHostPortGenericContainer<?> ftpContainer;
    static GenericContainer<?> sftpContainer;

    private static int passivePortSearchCursor = PASSIVE_PORT_SEARCH_FIRST;
    private static volatile boolean sftpSeeded;

    protected String ftpPassword;
    protected String ftpUsername;
    protected String ftpHostIp;

    @Value("${bytechef.test.ftp.password:}")
    private String configuredFtpPassword;
    @Value("${bytechef.test.ftp.username:}")
    private String configuredFtpUsername;
    @Value("${bytechef.test.ftp.host.ip:}")
    private String configuredFtpHostIp;

    @BeforeAll
    void resolveConfiguration() {
        ftpPassword = valueOrDefault(configuredFtpPassword, "T3st@Pass123");
        ftpUsername = valueOrDefault(configuredFtpUsername, "ftpuser");

        InetAddress loopbackAddress = InetAddress.getLoopbackAddress();

        ftpHostIp = valueOrDefault(configuredFtpHostIp, loopbackAddress.getHostAddress());
    }

    @BeforeEach
    @Timeout(value = PREPARE_TIMEOUT_SECONDS, unit = TimeUnit.SECONDS)
    void prepareServers() {
        prepareFtpServer();
        prepareSftpServer();
    }

    protected static FileEntry mockFileEntry() throws Exception {
        FileEntry mockFileEntry = mock(FileEntry.class);
        when(mockFileEntry.getName()).thenReturn(TEST_FILE_NAME);
        when(mockFileEntry.getExtension()).thenReturn("txt");
        when(mockFileEntry.getMimeType()).thenReturn("text/plain");

        return mockFileEntry;
    }

    protected static ActionContext getMockActionContext(FileEntry mockFileEntry) {
        ActionContext context = mock(ActionContext.class);
        Context.File mockContextFile = mock(Context.File.class);

        when(
            mockContextFile.getInputStream(mockFileEntry)).thenAnswer(
                invocation -> new ByteArrayInputStream(TEST_CONTENT.getBytes(StandardCharsets.UTF_8)));

        when(
            context.file(any())).thenAnswer(
                invocation -> {
                    Context.ContextFunction<Context.File, FileEntry> fileFunction = invocation.getArgument(0);

                    return fileFunction.apply(mockContextFile);
                });

        return context;
    }

    private void prepareFtpServer() {
        StringBuilder diagnostics = new StringBuilder();

        for (int attempt = 1; attempt <= PREPARE_MAX_ATTEMPTS; attempt++) {
            try {
                if (ftpContainer == null || !ftpContainer.isRunning()) {
                    startFtpContainer();
                }

                seedFtpFiles();

                return;
            } catch (Exception exception) {
                diagnostics.append(System.lineSeparator())
                    .append("attempt ")
                    .append(attempt)
                    .append(": ")
                    .append(exception);

                discardFtpContainer();
            }
        }

        throw new IllegalStateException(
            "FTP server could not be prepared after " + PREPARE_MAX_ATTEMPTS + " attempts:" + diagnostics);
    }

    private void prepareSftpServer() {
        StringBuilder diagnostics = new StringBuilder();

        for (int attempt = 1; attempt <= PREPARE_MAX_ATTEMPTS; attempt++) {
            try {
                if (sftpContainer == null || !sftpContainer.isRunning()) {
                    startSftpContainer();
                }

                if (!sftpSeeded) {
                    seedSftpFiles();

                    sftpSeeded = true;
                }

                return;
            } catch (Exception exception) {
                diagnostics.append(System.lineSeparator())
                    .append("attempt ")
                    .append(attempt)
                    .append(": ")
                    .append(exception);

                discardSftpContainer();
            }
        }

        throw new IllegalStateException(
            "SFTP server could not be prepared after " + PREPARE_MAX_ATTEMPTS + " attempts:" + diagnostics);
    }

    @SuppressWarnings("deprecation")
    private void startFtpContainer() {
        int passivePortMin = reservePassivePortRange();
        int passivePortMax = passivePortMin + PASSIVE_PORT_COUNT - 1;

        FixedHostPortGenericContainer<?> container = new FixedHostPortGenericContainer<>(FTP_IMAGE)
            .withEnv("USERS", ftpUsername + "|" + ftpPassword)
            .withCommand(
                "sh", "-c",
                "exec vsftpd /etc/vsftpd/vsftpd.conf -obackground=NO -opasv_address=" + ftpHostIp
                    + " -opasv_min_port=" + passivePortMin + " -opasv_max_port=" + passivePortMax)
            .withExposedPorts(FTP_CONTROL_PORT)
            .waitingFor(Wait.forListeningPorts(FTP_CONTROL_PORT))
            .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT);

        for (int port = passivePortMin; port <= passivePortMax; port++) {
            container.withFixedExposedPort(port, port);
        }

        container.start();

        ftpContainer = container;

        awaitFtpReady();
    }

    private void startSftpContainer() {
        GenericContainer<?> container = new GenericContainer<>(SFTP_IMAGE)
            .withCommand(ftpUsername + ":" + ftpPassword + ":::" + SFTP_UPLOAD_DIR)
            .withExposedPorts(SFTP_PORT)
            .withStartupTimeout(CONTAINER_STARTUP_TIMEOUT);

        container.start();

        sftpContainer = container;
        sftpSeeded = false;

        awaitSftpReady();
    }

    private void discardFtpContainer() {
        if (ftpContainer != null) {
            ftpContainer.stop();

            ftpContainer = null;
        }
    }

    private void discardSftpContainer() {
        if (sftpContainer != null) {
            sftpContainer.stop();

            sftpContainer = null;
        }

        sftpSeeded = false;
    }

    private void awaitFtpReady() {
        long deadline = System.nanoTime() + READINESS_TIMEOUT.toNanos();
        Exception lastFailure = null;

        while (System.nanoTime() < deadline) {
            try {
                listFileNamesViaFtp();

                return;
            } catch (Exception exception) {
                lastFailure = exception;

                sleep(READINESS_POLL_INTERVAL);
            }
        }

        throw new IllegalStateException(
            "FTP server did not become ready within " + READINESS_TIMEOUT + describeContainerLogs(ftpContainer),
            lastFailure);
    }

    private void awaitSftpReady() {
        long deadline = System.nanoTime() + READINESS_TIMEOUT.toNanos();
        Exception lastFailure = null;

        while (System.nanoTime() < deadline) {
            SSHClient sshClient = null;

            try {
                sshClient = connectSftp();

                return;
            } catch (Exception exception) {
                lastFailure = exception;

                sleep(READINESS_POLL_INTERVAL);
            } finally {
                disconnectQuietly(sshClient);
            }
        }

        throw new IllegalStateException(
            "SFTP server did not become ready within " + READINESS_TIMEOUT + describeContainerLogs(sftpContainer),
            lastFailure);
    }

    private void seedFtpFiles() throws Exception {
        String homeDirectory = "/ftp/" + ftpUsername;
        String testFilePath = homeDirectory + "/" + TEST_FILE_NAME;

        StringBuilder script = new StringBuilder("set -e; rm -rf ")
            .append(homeDirectory)
            .append("/*; printf '%s' '")
            .append(TEST_CONTENT)
            .append("' > ")
            .append(testFilePath)
            .append(';');

        for (String suffix : SEEDED_FILE_SUFFIXES) {
            script.append(" cp ")
                .append(testFilePath)
                .append(' ')
                .append(homeDirectory)
                .append('/')
                .append(TEST_FILE_BASE_NAME)
                .append('-')
                .append(suffix)
                .append(TEST_FILE_BASE_EXTENSION)
                .append(';');
        }

        script.append(" yes '")
            .append(LARGE_TEST_CONTENT_LINE)
            .append("' | head -n ")
            .append(LARGE_TEST_CONTENT_LINE_COUNT)
            .append(" > ")
            .append(homeDirectory)
            .append('/')
            .append(LARGE_TEST_FILE_NAME)
            .append(';');

        script.append(" chown -R ")
            .append(ftpUsername)
            .append(':')
            .append(ftpUsername)
            .append(' ')
            .append(homeDirectory);

        ExecResult result = ftpContainer.execInContainer("sh", "-c", script.toString());

        if (result.getExitCode() != 0) {
            throw new IllegalStateException(
                "Failed to seed FTP test files inside container: " + result.getStderr());
        }

        List<String> fileNames = listFileNamesViaFtp();

        if (fileNames.size() != SEEDED_FILE_COUNT) {
            throw new IllegalStateException(
                "Expected " + SEEDED_FILE_COUNT + " seeded FTP files but found " + fileNames);
        }
    }

    private void seedSftpFiles() throws Exception {
        SSHClient sshClient = connectSftp();

        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            putSftpFile(sftpClient, SFTP_REMOTE_PATH, TEST_FILE_NAME, TEST_CONTENT);
            putSftpFile(sftpClient, LARGE_SFTP_REMOTE_PATH, LARGE_TEST_FILE_NAME, LARGE_TEST_CONTENT);
        } finally {
            disconnectQuietly(sshClient);
        }
    }

    private static void putSftpFile(SFTPClient sftpClient, String remotePath, String fileName, String content)
        throws IOException {

        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);

        sftpClient.put(new InMemorySourceFile() {

            @Override
            public String getName() {
                return fileName;
            }

            @Override
            public long getLength() {
                return contentBytes.length;
            }

            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(contentBytes);
            }
        }, remotePath);
    }

    private SSHClient connectSftp() throws IOException {
        SSHClient sshClient = new SSHClient();

        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.setConnectTimeout((int) PROBE_TIMEOUT.toMillis());
        sshClient.setTimeout((int) PROBE_TIMEOUT.toMillis());

        try {
            sshClient.connect(ftpHostIp, sftpContainer.getMappedPort(SFTP_PORT));
            sshClient.authPassword(ftpUsername, ftpPassword);

            return sshClient;
        } catch (IOException ioException) {
            disconnectQuietly(sshClient);

            throw ioException;
        }
    }

    private List<String> listFileNamesViaFtp() throws IOException {
        FTPClient ftpClient = new FTPClient();

        ftpClient.setConnectTimeout((int) PROBE_TIMEOUT.toMillis());
        ftpClient.setDefaultTimeout((int) PROBE_TIMEOUT.toMillis());
        ftpClient.setDataTimeout(PROBE_TIMEOUT);

        try {
            ftpClient.connect(ftpHostIp, ftpContainer.getMappedPort(FTP_CONTROL_PORT));

            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                throw new IOException("FTP server refused connection: " + ftpClient.getReplyString());
            }

            ftpClient.setSoTimeout((int) PROBE_TIMEOUT.toMillis());

            if (!ftpClient.login(ftpUsername, ftpPassword)) {
                throw new IOException("FTP login failed for user " + ftpUsername + ": " + ftpClient.getReplyString());
            }

            ftpClient.enterLocalPassiveMode();

            FTPFile[] files = ftpClient.listFiles("");

            if (!FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
                throw new IOException("FTP listing failed: " + ftpClient.getReplyString());
            }

            return Arrays.stream(files)
                .map(FTPFile::getName)
                .filter(name -> !".".equals(name) && !"..".equals(name))
                .toList();
        } finally {
            closeQuietly(ftpClient);
        }
    }

    private static synchronized int reservePassivePortRange() {
        int candidate = passivePortSearchCursor;

        while (candidate + PASSIVE_PORT_COUNT - 1 <= PASSIVE_PORT_SEARCH_LAST) {
            if (isPortRangeFree(candidate)) {
                passivePortSearchCursor = candidate + PASSIVE_PORT_COUNT;

                return candidate;
            }

            candidate += PASSIVE_PORT_COUNT;
        }

        throw new IllegalStateException(
            "No free run of " + PASSIVE_PORT_COUNT + " ports between " + passivePortSearchCursor + " and "
                + PASSIVE_PORT_SEARCH_LAST);
    }

    @SuppressFBWarnings(
        value = "UNENCRYPTED_SERVER_SOCKET",
        justification = "The socket is only bound to probe port availability; nothing is ever served on it")
    private static boolean isPortRangeFree(int firstPort) {
        List<ServerSocket> serverSockets = new ArrayList<>();

        try {
            for (int port = firstPort; port < firstPort + PASSIVE_PORT_COUNT; port++) {
                ServerSocket serverSocket = new ServerSocket();

                serverSocket.bind(new InetSocketAddress(port));

                serverSockets.add(serverSocket);
            }

            return true;
        } catch (IOException ioException) {
            return false;
        } finally {
            for (ServerSocket serverSocket : serverSockets) {
                closeQuietly(serverSocket);
            }
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

    private static String valueOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value;
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

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private static void closeQuietly(FTPClient ftpClient) {
        if (ftpClient.isConnected()) {
            try {
                ftpClient.disconnect();
            } catch (IOException ioException) {
                // Intentionally ignored - best-effort cleanup of a probe connection
            }
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private static void closeQuietly(ServerSocket serverSocket) {
        try {
            serverSocket.close();
        } catch (IOException ioException) {
            // Intentionally ignored - the port probe released what it could
        }
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

    protected static class TestContextImpl implements ActionContext {

        private File file;

        TestContextImpl() {
            this.file = new TestFileImpl();
        }

        public byte[] getCapturedBytes(FileEntry fileEntry) throws IOException {
            return file.readAllBytes(fileEntry);
        }

        @Override
        public <R> R converter(ContextFunction<Converter, R> converterFunction) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public <R> R encoder(ContextFunction<Encoder, R> encoderFunction) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public <R> R escaper(ContextFunction<Escaper, R> escaperFunction) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public <R> R file(ContextFunction<File, R> fileFunction) {
            try {
                return fileFunction.apply(file);
            } catch (Exception e) {
                throw new RuntimeException("Unable to apply function", e);
            }
        }

        @Override
        public <R> R http(ContextFunction<Http, R> httpFunction) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public boolean isEditorEnvironment() {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public <R> R json(ContextFunction<Json, R> jsonFunction) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public void log(ContextConsumer<Log> logConsumer) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public <R> R mimeType(ContextFunction<MimeType, R> mimeTypeFunction) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public <R> R outputSchema(ContextFunction<OutputSchema, R> outputSchemaFunction) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public String getTraceId() {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public <R> R xml(ContextFunction<Xml, R> xmlFunction) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public Approval.Links approval(ContextFunction<Approval, Approval.Links> approvalFunction) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public <R> R data(ContextFunction<Data, R> dataFunction) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public void event(Consumer<Event> eventConsumer) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public void suspend(Suspend suspend) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }
    }

    static class TestFileImpl implements Context.File {
        private String extension = "txt";
        private String mimeType = "txt/txt";
        private String name;
        private String url = "file://text-file.txt";
        private String data;

        @Override
        public long getContentLength(FileEntry fileEntry) {
            return data.length();
        }

        @Override
        public InputStream getInputStream(FileEntry fileEntry) {
            return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public OutputStream getOutputStream(FileEntry fileEntry) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public String readToString(FileEntry fileEntry) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public FileEntry storeContent(String fileName, String fileData) {
            data = fileData;
            name = fileName;

            return new FileEntry() {

                @Override
                public String getExtension() {
                    return extension;
                }

                @Override
                public String getMimeType() {
                    return mimeType;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getUrl() {
                    return url;
                }
            };
        }

        @Override
        public File toTempFile(FileEntry fileEntry) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public Path toTempFilePath(FileEntry fileEntry) {
            throw new UnsupportedOperationException("Disabled in test mode");
        }

        @Override
        public byte[] readAllBytes(FileEntry fileEntry) throws IOException {
            InputStream inputStream = getInputStream(fileEntry);

            return inputStream.readAllBytes();
        }

        @Override
        public FileEntry storeContent(String fileName, InputStream inputStream) {
            this.name = fileName;

            try {
                return storeContent(
                    fileName, new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            } catch (Exception exception) {
                throw new RuntimeException("Unable to store file " + fileName, exception);
            }
        }
    }

    @Configuration
    static class TestConfig {
    }
}
