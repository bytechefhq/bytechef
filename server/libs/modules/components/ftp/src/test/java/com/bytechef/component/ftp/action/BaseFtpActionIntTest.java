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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import org.junit.jupiter.api.AfterAll;
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
    @SuppressWarnings("deprecation")
    protected FixedHostPortGenericContainer<?> ftpContainer;
    protected GenericContainer<?> sftpContainer;

    @BeforeAll
    void setUpContainers() throws Exception {
        ftpContainer = new FixedHostPortGenericContainer<>("delfer/alpine-ftp-server:latest")
            .withEnv("USERS", ftpUsername + "|" + ftpPassword)
            .withEnv("ADDRESS", ftpHostIp)
            .withEnv("MIN_PORT", String.valueOf(PASSIVE_DATA_PORT))
            .withEnv("MAX_PORT", String.valueOf(PASSIVE_DATA_PORT))
            .withExposedPorts(21)
            .withFixedExposedPort(PASSIVE_DATA_PORT, PASSIVE_DATA_PORT);

        ftpContainer.start();

        sftpContainer = new GenericContainer<>("atmoz/sftp:latest")
            .withCommand(ftpUsername + ":" + ftpPassword + ":::" + SFTP_UPLOAD_DIR)
            .withExposedPorts(22);

        sftpContainer.start();

        uploadTestFileViaFtp();
        uploadTestFileViaSftp();

        copyTestFileViaFtp("for-delete");

        copyTestFileViaFtp("001");
        copyTestFileViaFtp("002");
        copyTestFileViaFtp("003");
        copyTestFileViaFtp("004");
        copyTestFileViaFtp("005");
    }

    @AfterAll
    void tearDownContainers() {
        if (ftpContainer != null) {
            ftpContainer.stop();
        }

        if (sftpContainer != null) {
            sftpContainer.stop();
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
