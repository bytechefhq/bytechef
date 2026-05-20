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

import static com.bytechef.component.ftp.constant.FtpConstants.FILE_ENTRY;
import static com.bytechef.component.ftp.constant.FtpConstants.HOST;
import static com.bytechef.component.ftp.constant.FtpConstants.PASSIVE_MODE;
import static com.bytechef.component.ftp.constant.FtpConstants.PASSWORD;
import static com.bytechef.component.ftp.constant.FtpConstants.PATH;
import static com.bytechef.component.ftp.constant.FtpConstants.PORT;
import static com.bytechef.component.ftp.constant.FtpConstants.SFTP;
import static com.bytechef.component.ftp.constant.FtpConstants.USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Context;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration tests for {@link FtpUploadFileAction} using real FTP and SFTP servers spun up via Testcontainers.
 *
 * @author Igor Beslic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
@Testcontainers
public class FtpUploadActionIntTest {

    /**
     * Shared credentials for both containers. Password must satisfy Alpine Linux's strength requirements used by
     * {@code delfer/alpine-ftp-server}'s entrypoint.
     */
    @Value("${bytechef.test.ftp.password}")
    private String ftpPassword;
    @Value("${bytechef.test.ftp.username}")
    private String ftpUsername;
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    @Value("${bytechef.test.ftp.host.ip}")
    private String ftpHostIp;

    /**
     * Single passive-mode data port shared by the FTP container and the host. Using a fixed port is required because
     * the FTP server embeds this port in its PASV response, so the host-side port must match the container port
     * exactly.
     */
    private static final int PASSIVE_DATA_PORT = 30121;

    private static final String TEST_CONTENT = "Hello from FTP Integration Test!";
    private static final String TEST_FILENAME = "test-document.txt";

    /**
     * Pure-FTPd container. The {@code ADDRESS} env-var forces the PASV response to advertise {@code 127.0.0.1} so that
     * the FTP client always connects to the host loopback. The passive port range is collapsed to a single value equal
     * to {@link #PASSIVE_DATA_PORT} to make the fixed host-port binding deterministic.
     * <p>
     * {@link org.testcontainers.containers.FixedHostPortGenericContainer} is used because FTP passive mode requires the
     * host port and container port to be identical: the server embeds the port number in the PASV response, so the FTP
     * client connects directly to that port on the host — dynamic port mapping would break this contract.
     */
    @Container
    @SuppressWarnings("deprecation")
    protected FixedHostPortGenericContainer<?> ftpContainer =
        new FixedHostPortGenericContainer<>("delfer/alpine-ftp-server:latest")
            .withEnv("USERS", ftpUsername + "|" + ftpPassword)
            .withEnv("ADDRESS", ftpHostIp)
            .withEnv("MIN_PORT", String.valueOf(PASSIVE_DATA_PORT))
            .withEnv("MAX_PORT", String.valueOf(PASSIVE_DATA_PORT))
            .withExposedPorts(21)
            .withFixedExposedPort(PASSIVE_DATA_PORT, PASSIVE_DATA_PORT);

    @Test
    void testUpload() throws Exception {
        Assertions.assertNotNull(ftpPassword);
        Assertions.assertNotNull(ftpUsername);
        Assertions.assertNotNull(ftpHostIp);

        Parameters connectionParameters = MockParametersFactory.create(Map.of(
            HOST, ftpHostIp,
            PORT, ftpContainer.getMappedPort(21),
            USERNAME, ftpUsername,
            PASSWORD, ftpPassword,
            PASSIVE_MODE, true,
            SFTP, false));

        FileEntry mockFileEntry = mockFileEntry();

        Parameters inputParameters =
            MockParametersFactory.create(Map.of(PATH, "uploaded-" + TEST_FILENAME, FILE_ENTRY, mockFileEntry));

        Map<String, Object> remoteFileSystemPerformResult =
            FtpUploadFileAction.perform(inputParameters, connectionParameters, getMockActionContext(mockFileEntry));

        assertEquals(2, remoteFileSystemPerformResult.size());

        Assertions.assertTrue(remoteFileSystemPerformResult.containsKey("remotePath"));
        Assertions.assertTrue((Boolean) remoteFileSystemPerformResult.get("success"));
    }

    private static FileEntry mockFileEntry() throws Exception {
        FileEntry mockFileEntry = mock(FileEntry.class);
        when(mockFileEntry.getName()).thenReturn(TEST_FILENAME);
        when(mockFileEntry.getExtension()).thenReturn("txt");
        when(mockFileEntry.getMimeType()).thenReturn("text/plain");

        return mockFileEntry;
    }

    private static ActionContext getMockActionContext(FileEntry mockFileEntry) {
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
}
