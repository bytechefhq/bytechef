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

import static com.bytechef.component.ftp.constant.FtpConstants.HOST;
import static com.bytechef.component.ftp.constant.FtpConstants.PASSIVE_MODE;
import static com.bytechef.component.ftp.constant.FtpConstants.PASSWORD;
import static com.bytechef.component.ftp.constant.FtpConstants.PATH;
import static com.bytechef.component.ftp.constant.FtpConstants.PORT;
import static com.bytechef.component.ftp.constant.FtpConstants.SFTP;
import static com.bytechef.component.ftp.constant.FtpConstants.USERNAME;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.Context.ContextFunction;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for {@link FtpDownloadFileAction} using real FTP and SFTP servers spun up via Testcontainers.
 *
 * <p>
 * How to extend these tests for new scenarios: <br>
 * 1. Add a new {@code @Test} method. <br>
 * 2. Build {@link Parameters} with {@link MockParametersFactory#create(Map)} using the constants from
 * {@link com.bytechef.component.ftp.constant.FtpConstants}. <br>
 * 3. Use {@link #buildCapturingContext} to obtain a mock {@link ActionContext} that captures the downloaded bytes and
 * filename. <br>
 * 4. Call {@link FtpDownloadFileAction#perform} and assert on the captured values. <br>
 * <br>
 * FTP container configuration: change {@link #PASSIVE_DATA_PORT}, {@link #FTP_USERNAME}, or {@link #FTP_PASSWORD} and
 * adjust the environment variables passed to {@code ftpContainer} accordingly. <br>
 * SFTP container configuration: modify the {@code --command} argument of {@code sftpContainer} using the format
 * {@code user:password[:::[dir]]} as documented in the {@code atmoz/sftp} image.
 *
 * @author Igor Beslic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
@Testcontainers
public class FtpDownloadFileActionIntTest {

    /**
     * Shared credentials for both containers. Password must satisfy Alpine Linux's strength requirements used by
     * {@code delfer/alpine-ftp-server}'s entrypoint.
     */
    private static final String FTP_PASSWORD = "T3st@Pass123";
    private static final String FTP_USERNAME = "ftpuser";

    /**
     * Single passive-mode data port shared by the FTP container and the host. Using a fixed port is required because
     * the FTP server embeds this port in its PASV response, so the host-side port must match the container port
     * exactly.
     */
    private static final int PASSIVE_DATA_PORT = 30121;

    private static final String TEST_CONTENT = "Hello from FTP Integration Test!";
    private static final String TEST_FILENAME = "test-document.txt";

    /**
     * {@code delfer/alpine-ftp-server} runs Pure-FTPd without per-user chroot. After login the server CWD is the user's
     * home directory ({@code /ftp/<username>/}). Absolute paths resolve against the real filesystem root, so a relative
     * path (no leading slash) is used here to land in the correct directory.
     */
    private static final String FTP_REMOTE_PATH = TEST_FILENAME;
    private static final String SFTP_UPLOAD_DIR = "upload";
    private static final String SFTP_REMOTE_PATH = "/" + SFTP_UPLOAD_DIR + "/" + TEST_FILENAME;

    /**
     * Pure-FTPd container. The {@code ADDRESS} env-var forces the PASV response to advertise {@code 127.0.0.1} so that
     * the FTP client always connects to the host loopback. The passive port range is collapsed to a single value equal
     * to {@link #PASSIVE_DATA_PORT} to make the fixed host-port binding deterministic.
     * <p>
     * {@link FixedHostPortGenericContainer} is used because FTP passive mode requires the host port and container port
     * to be identical: the server embeds the port number in the PASV response, so the FTP client connects directly to
     * that port on the host — dynamic port mapping would break this contract.
     */
    @Container
    @SuppressWarnings("deprecation")
    static final FixedHostPortGenericContainer<?> ftpContainer =
        new FixedHostPortGenericContainer<>("delfer/alpine-ftp-server:latest")
            .withEnv("USERS", FTP_USERNAME + "|" + FTP_PASSWORD)
            .withEnv("ADDRESS", "127.0.0.1")
            .withEnv("MIN_PORT", String.valueOf(PASSIVE_DATA_PORT))
            .withEnv("MAX_PORT", String.valueOf(PASSIVE_DATA_PORT))
            .withExposedPorts(21)
            .withFixedExposedPort(PASSIVE_DATA_PORT, PASSIVE_DATA_PORT);

    /**
     * OpenSSH/SFTP container (atmoz/sftp). The command argument creates user {@code ftpuser} and an {@code upload}
     * subdirectory inside the user's chroot. SFTP uses a single SSH port so there is no passive-mode complexity.
     */
    @Container
    static final GenericContainer<?> sftpContainer = new GenericContainer<>("atmoz/sftp:latest")
        .withCommand(FTP_USERNAME + ":" + FTP_PASSWORD + ":::" + SFTP_UPLOAD_DIR)
        .withExposedPorts(22);

    @BeforeAll
    static void uploadTestFiles() throws Exception {
        uploadTestFileViaFtp();
        uploadTestFileViaSftp();
    }

    @Test
    void testDownloadFileViaFtp() throws Exception {
        Parameters connectionParameters = MockParametersFactory.create(Map.of(
            HOST, "127.0.0.1",
            PORT, ftpContainer.getMappedPort(21),
            USERNAME, FTP_USERNAME,
            PASSWORD, FTP_PASSWORD,
            PASSIVE_MODE, true,
            SFTP, false));
        Parameters inputParameters = MockParametersFactory.create(Map.of(PATH, FTP_REMOTE_PATH));

        AtomicReference<String> capturedFilename = new AtomicReference<>();
        AtomicReference<byte[]> capturedContent = new AtomicReference<>();
        FileEntry mockFileEntry = mock(FileEntry.class);
        ActionContext context = buildCapturingContext(capturedFilename, capturedContent, mockFileEntry);

        FileEntry result = FtpDownloadFileAction.perform(inputParameters, connectionParameters, context);

        assertSame(mockFileEntry, result);
        assertEquals(TEST_FILENAME, capturedFilename.get());
        assertArrayEquals(TEST_CONTENT.getBytes(StandardCharsets.UTF_8), capturedContent.get());
    }

    @Test
    void testDownloadFileViaSftp() throws Exception {
        Parameters connectionParameters = MockParametersFactory.create(Map.of(
            HOST, "127.0.0.1",
            PORT, sftpContainer.getMappedPort(22),
            USERNAME, FTP_USERNAME,
            PASSWORD, FTP_PASSWORD,
            SFTP, true));
        Parameters inputParameters = MockParametersFactory.create(Map.of(PATH, SFTP_REMOTE_PATH));

        AtomicReference<String> capturedFilename = new AtomicReference<>();
        AtomicReference<byte[]> capturedContent = new AtomicReference<>();
        FileEntry mockFileEntry = mock(FileEntry.class);
        ActionContext context = buildCapturingContext(capturedFilename, capturedContent, mockFileEntry);

        FileEntry result = FtpDownloadFileAction.perform(inputParameters, connectionParameters, context);

        assertSame(mockFileEntry, result);
        assertEquals(TEST_FILENAME, capturedFilename.get());
        assertArrayEquals(TEST_CONTENT.getBytes(StandardCharsets.UTF_8), capturedContent.get());
    }

    /**
     * Creates a mock {@link ActionContext} whose {@code file()} method captures the filename and raw bytes passed to
     * {@code storeContent} so that tests can assert on the downloaded file content without needing a real file-storage
     * backend.
     *
     * <p>
     * To add assertions on additional {@link Context.File} interactions, inject a pre-configured {@code Context.File}
     * mock instead of the one built internally here.
     */
    private static ActionContext buildCapturingContext(
        AtomicReference<String> capturedFilename,
        AtomicReference<byte[]> capturedContent,
        FileEntry mockFileEntry) throws Exception {

        ActionContext context = mock(ActionContext.class);
        Context.File mockFile = mock(Context.File.class);

        when(mockFile.storeContent(anyString(), any(InputStream.class))).thenAnswer(invocation -> {
            capturedFilename.set(invocation.getArgument(0));

            InputStream inputStream = invocation.getArgument(1);

            capturedContent.set(inputStream.readAllBytes());

            return mockFileEntry;
        });

        when(context.file(any())).thenAnswer(invocation -> {
            ContextFunction<Context.File, FileEntry> fileFunction = invocation.getArgument(0);

            return fileFunction.apply(mockFile);
        });

        return context;
    }

    /**
     * Seeds the FTP test file directly inside the container using {@code execInContainer} rather than the FTP protocol.
     * This sidesteps passive-mode port-mapping complexity (the PASV data port must match host:container exactly) while
     * still leaving the actual download path — exercised by {@link #testDownloadFileViaFtp} — as the thing under test.
     */
    private static void uploadTestFileViaFtp() throws Exception {
        String filePath = "/ftp/" + FTP_USERNAME + "/" + TEST_FILENAME;

        var result = ftpContainer.execInContainer(
            "sh", "-c",
            "printf '%s' '" + TEST_CONTENT + "' > " + filePath
                + " && chown " + FTP_USERNAME + ":" + FTP_USERNAME + " " + filePath);

        if (result.getExitCode() != 0) {
            throw new IllegalStateException(
                "Failed to write FTP test file inside container: " + result.getStderr());
        }
    }

    private static void uploadTestFileViaSftp() throws Exception {
        SSHClient sshClient = new SSHClient();

        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect("127.0.0.1", sftpContainer.getMappedPort(22));
        sshClient.authPassword(FTP_USERNAME, FTP_PASSWORD);

        try (SFTPClient sftpClient = sshClient.newSFTPClient()) {
            byte[] contentBytes = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);

            sftpClient.put(new InMemorySourceFile() {

                @Override
                public String getName() {
                    return TEST_FILENAME;
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
