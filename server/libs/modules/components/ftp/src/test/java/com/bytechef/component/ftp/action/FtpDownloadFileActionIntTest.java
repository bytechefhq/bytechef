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
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

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
public class FtpDownloadFileActionIntTest extends BaseFtpActionIntTest {

    @Test
    void testDownloadFileViaFtp() throws Exception {
        Parameters connectionParameters = MockParametersFactory.create(Map.of(
            HOST, ftpHostIp,
            PORT, ftpContainer.getMappedPort(21),
            USERNAME, ftpUsername,
            PASSWORD, ftpPassword,
            PASSIVE_MODE, true,
            SFTP, false));
        Parameters inputParameters = MockParametersFactory.create(Map.of(PATH, FTP_REMOTE_PATH));

        AtomicReference<String> capturedFilename = new AtomicReference<>();
        AtomicReference<byte[]> capturedContent = new AtomicReference<>();
        FileEntry mockFileEntry = mock(FileEntry.class);
        ActionContext context = buildCapturingContext(capturedFilename, capturedContent, mockFileEntry);

        FileEntry result = FtpDownloadFileAction.perform(inputParameters, connectionParameters, context);

        assertSame(mockFileEntry, result);
        assertEquals(TEST_FILE_NAME, capturedFilename.get());
        assertArrayEquals(TEST_CONTENT.getBytes(StandardCharsets.UTF_8), capturedContent.get());
    }

    @Test
    void testDownloadFileViaSftp() throws Exception {
        Parameters connectionParameters = MockParametersFactory.create(Map.of(
            HOST, ftpHostIp,
            PORT, sftpContainer.getMappedPort(22),
            USERNAME, ftpUsername,
            PASSWORD, ftpPassword,
            SFTP, true));
        Parameters inputParameters = MockParametersFactory.create(Map.of(PATH, SFTP_REMOTE_PATH));

        AtomicReference<String> capturedFilename = new AtomicReference<>();
        AtomicReference<byte[]> capturedContent = new AtomicReference<>();
        FileEntry mockFileEntry = mock(FileEntry.class);
        ActionContext context = buildCapturingContext(capturedFilename, capturedContent, mockFileEntry);

        FileEntry result = FtpDownloadFileAction.perform(inputParameters, connectionParameters, context);

        assertSame(mockFileEntry, result);
        assertEquals(TEST_FILE_NAME, capturedFilename.get());
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

}
