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

import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Integration tests for {@link FtpUploadFileAction} using real FTP and SFTP servers spun up via Testcontainers.
 *
 * @author Igor Beslic
 */
@SpringJUnitConfig(FtpUploadActionIntTest.TestConfig.class)
public class FtpUploadActionIntTest extends BaseFtpActionIntTest {

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
            MockParametersFactory.create(Map.of(PATH, "uploaded-" + TEST_FILE_NAME, FILE_ENTRY, mockFileEntry));

        Map<String, Object> remoteFileSystemPerformResult =
            FtpUploadFileAction.perform(inputParameters, connectionParameters, getMockActionContext(mockFileEntry));

        assertEquals(2, remoteFileSystemPerformResult.size());

        Assertions.assertTrue(remoteFileSystemPerformResult.containsKey("remotePath"));
        Assertions.assertTrue((Boolean) remoteFileSystemPerformResult.get("success"));
    }
}
