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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;

/**
 * Integration tests for {@link FtpUploadFileAction} using real FTP and SFTP servers spun up via Testcontainers.
 *
 * @author Igor Beslic
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringJUnitConfig(BaseFtpActionIntTest.TestConfig.class)
public class BaseFtpActionIntTest {

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

    /**
     * Shared credentials for both containers. Password must satisfy Alpine Linux's strength requirements used by
     * {@code delfer/alpine-ftp-server}'s entrypoint.
     */
    @Value("${bytechef.test.ftp.password}")
    protected String ftpPassword;
    @Value("${bytechef.test.ftp.username}")
    protected String ftpUsername;
    @Value("${bytechef.test.ftp.host.ip}")
    protected String ftpHostIp;

    /**
     * Single passive-mode data port shared by the FTP container and the host. Using a fixed port is required because
     * the FTP server embeds this port in its PASV response, so the host-side port must match the container port
     * exactly.
     */
    protected static final int PASSIVE_DATA_PORT = 30121;
    private static final int FTP_CONTAINER_START_MAX_ATTEMPTS = 6;
    private static final long FTP_CONTAINER_START_RETRY_DELAY_MILLIS = 2000;
    protected static final String TEST_CONTENT = "Hello from FTP Integration Test!";
    protected static final String TEST_FILE_BASE_NAME = "test-document";
    protected static final String TEST_FILE_BASE_EXTENSION = ".txt";
    protected static final String TEST_FILE_NAME = TEST_FILE_BASE_NAME + TEST_FILE_BASE_EXTENSION;
    protected static final String FTP_REMOTE_PATH = TEST_FILE_NAME;
    protected static final String SFTP_UPLOAD_DIR = "upload";
    protected static final String SFTP_REMOTE_PATH = "/" + SFTP_UPLOAD_DIR + "/" + TEST_FILE_NAME;

    /**
     * Pure-FTPd container. The {@code ADDRESS} env-var forces the PASV response to advertise {@code 127.0.0.1} so that
     * the FTP client always connects to the host loopback. The passive port range is collapsed to a single value equal
     * to {@link #PASSIVE_DATA_PORT} to make the fixed host-port binding deterministic.
     * <p>
     * {@link org.testcontainers.containers.FixedHostPortGenericContainer} is used because FTP passive mode requires the
     * host port and container port to be identical: the server embeds the port number in the PASV response, so the FTP
     * client connects directly to that port on the host — dynamic port mapping would break this contract.
     */
    static FixedHostPortGenericContainer<?> ftpContainer;
    static GenericContainer<?> sftpContainer;

    @BeforeAll
    void setUpContainers() throws Exception {
        startContainersOnce();

        resetFtpFiles();

        uploadTestFileViaFtp();
        uploadTestFileViaSftp();

        copyTestFileViaFtp("for-delete");

        copyTestFileViaFtp("001");
        copyTestFileViaFtp("002");
        copyTestFileViaFtp("003");
        copyTestFileViaFtp("004");
        copyTestFileViaFtp("005");
    }

    private void startContainersOnce() throws InterruptedException {
        if (ftpContainer == null) {
            ftpContainer = startFtpContainer();
        }

        if (sftpContainer == null) {
            GenericContainer<?> container = new GenericContainer<>("atmoz/sftp:latest")
                .withCommand(ftpUsername + ":" + ftpPassword + ":::" + SFTP_UPLOAD_DIR)
                .withExposedPorts(22);

            container.start();

            sftpContainer = container;
        }
    }

    @SuppressWarnings("deprecation")
    private FixedHostPortGenericContainer<?> startFtpContainer() throws InterruptedException {
        RuntimeException startException = null;

        for (int attempt = 1; attempt <= FTP_CONTAINER_START_MAX_ATTEMPTS; attempt++) {
            FixedHostPortGenericContainer<?> container =
                new FixedHostPortGenericContainer<>("delfer/alpine-ftp-server:latest")
                    .withEnv("USERS", ftpUsername + "|" + ftpPassword)
                    .withEnv("ADDRESS", ftpHostIp)
                    .withEnv("MIN_PORT", String.valueOf(PASSIVE_DATA_PORT))
                    .withEnv("MAX_PORT", String.valueOf(PASSIVE_DATA_PORT))
                    .withExposedPorts(21)
                    .withFixedExposedPort(PASSIVE_DATA_PORT, PASSIVE_DATA_PORT);

            try {
                container.start();

                return container;
            } catch (RuntimeException exception) {
                startException = exception;

                container.stop();

                Thread.sleep(FTP_CONTAINER_START_RETRY_DELAY_MILLIS);
            }
        }

        throw new IllegalStateException(
            "FTP container failed to start after " + FTP_CONTAINER_START_MAX_ATTEMPTS + " attempts", startException);
    }

    protected void resetFtpFiles() throws Exception {
        var result = ftpContainer.execInContainer("sh", "-c", "rm -rf /ftp/" + ftpUsername + "/*");

        if (result.getExitCode() != 0) {
            throw new IllegalStateException("Failed to clean FTP test files inside container: " + result.getStderr());
        }
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

    /**
     * Seeds the FTP test file directly inside the container using {@code execInContainer} rather than the FTP protocol.
     * This sidesteps passive-mode port-mapping complexity (the PASV data port must match host:container exactly) while
     * still leaving the actual download path.
     */
    protected void copyTestFileViaFtp(String newFileSuffix) throws Exception {
        String filePath = "/ftp/" + ftpUsername + "/" + TEST_FILE_NAME;
        String copyOfFilePath =
            "/ftp/" + ftpUsername + "/" + TEST_FILE_BASE_NAME + "-" + newFileSuffix + TEST_FILE_BASE_EXTENSION;

        var result = ftpContainer.execInContainer(
            "sh", "-c",
            "cp " + filePath + " " + copyOfFilePath + " && chown " + ftpUsername + ":" + ftpUsername + " "
                + copyOfFilePath);

        if (result.getExitCode() != 0) {
            throw new IllegalStateException(
                "Failed to write FTP test file inside container: " + result.getStderr());
        }
    }

    /**
     * Seeds the FTP test file directly inside the container using {@code execInContainer} rather than the FTP protocol.
     * This sidesteps passive-mode port-mapping complexity (the PASV data port must match host:container exactly) while
     * still leaving the actual download path.
     */
    protected void uploadTestFileViaFtp() throws Exception {
        String filePath = "/ftp/" + ftpUsername + "/" + TEST_FILE_NAME;

        var result = ftpContainer.execInContainer(
            "sh", "-c",
            "printf '%s' '" + TEST_CONTENT + "' > " + filePath
                + " && chown " + ftpUsername + ":" + ftpUsername + " " + filePath);

        if (result.getExitCode() != 0) {
            throw new IllegalStateException(
                "Failed to write FTP test file inside container: " + result.getStderr());
        }
    }

    protected void uploadTestFileViaSftp() throws Exception {
        SSHClient sshClient = new SSHClient();

        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect(ftpHostIp, sftpContainer.getMappedPort(22));
        sshClient.authPassword(ftpUsername, ftpPassword);

        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            byte[] contentBytes = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);

            sftpClient.put(new InMemorySourceFile() {

                @Override
                public String getName() {
                    return TEST_FILE_NAME;
                }

                @Override
                public long getLength() {
                    return contentBytes.length;
                }

                @Override
                public InputStream getInputStream() {
                    return new ByteArrayInputStream(contentBytes);
                }
            }, SFTP_REMOTE_PATH);
        }

        sshClient.disconnect();
    }
}
