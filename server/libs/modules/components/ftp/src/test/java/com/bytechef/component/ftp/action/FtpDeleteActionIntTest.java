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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.test.definition.MockParametersFactory;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link FtpDeleteAction} using real FTP and SFTP servers spun up via Testcontainers.
 *
 * @author Igor Beslic
 */
public class FtpDeleteActionIntTest extends BaseFtpActionIntTest {

    @Test
    void testDelete() throws Exception {
        Parameters connectionParameters = MockParametersFactory.create(Map.of(
            HOST, ftpContainer.getHost(),
            PORT, ftpContainer.getMappedPort(21),
            USERNAME, ftpUsername,
            PASSWORD, ftpPassword,
            PASSIVE_MODE, true,
            SFTP, false));

        // list current directory content
        Parameters inputParameters =
            MockParametersFactory.create(Map.of(PATH, "test-document-for-delete.txt"));

        Map<String, Object> remoteFileSystemPerformResult =
            FtpDeleteAction.perform(inputParameters, connectionParameters, mock(ActionContext.class));

        assertEquals(2, remoteFileSystemPerformResult.size());

        Assertions.assertTrue(remoteFileSystemPerformResult.containsKey("deletedPath"));
        Assertions.assertTrue((Boolean) remoteFileSystemPerformResult.get("success"));
    }

}
