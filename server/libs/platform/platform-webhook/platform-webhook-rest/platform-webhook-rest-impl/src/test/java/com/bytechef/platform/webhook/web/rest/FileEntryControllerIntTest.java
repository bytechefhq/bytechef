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

package com.bytechef.platform.webhook.web.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bytechef.file.storage.domain.FileEntry;
import com.bytechef.file.storage.token.FileEntryTokens;
import com.bytechef.file.storage.token.FileEntryTokensAutoConfiguration;
import com.bytechef.platform.file.storage.TempFileStorage;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Integration test that boots {@link FileEntryTokensAutoConfiguration} with a real secret, mints a token via the real
 * {@link FileEntryTokens} bean, then exercises {@link FileEntryController} end-to-end to assert that the signed URL
 * round-trip returns the expected file content.
 *
 * <p>
 * Uses {@code ApplicationContextRunner} rather than a full {@code @SpringBootTest} slice so the test stays fast — no
 * Testcontainers or embedded server required.
 *
 * @author Ivica Cardic
 */
@SuppressFBWarnings("HARD_CODE_PASSWORD")
public class FileEntryControllerIntTest {

    private static final String SECRET = "dGVzdC1zZWNyZXQtMzItYnl0ZXMtYmFzZTY0LWVuY29kZWQ=";
    private static final byte[] FILE_CONTENT = "integration-test-content".getBytes(StandardCharsets.UTF_8);

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(FileEntryTokensAutoConfiguration.class))
        .withPropertyValues("bytechef.file-storage.signed-url.secret=" + SECRET);

    @Test
    public void testSignedTokenRoundTripReturns200WithFileContent() throws Exception {
        runner.run(context -> {
            FileEntryTokens fileEntryTokens = context.getBean(FileEntryTokens.class);
            FileEntry fileEntry = new FileEntry(
                "report.txt", "txt", "text/plain", "file:/temp/3a2b1c4d-5e6f-7081-9203-405060708090.txt");

            String token = fileEntryTokens.toSignedToken(fileEntry);

            TempFileStorage tempFileStorage = mock(TempFileStorage.class);

            when(tempFileStorage.getInputStream(any(FileEntry.class)))
                .thenReturn(new ByteArrayInputStream(FILE_CONTENT));

            FileEntryController controller = new FileEntryController(tempFileStorage, fileEntryTokens, false);
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();

            mockMvc.perform(get("/file-entries/" + token + "/content"))
                .andExpect(status().isOk())
                .andExpect(content().bytes(FILE_CONTENT));
        });
    }

    @Test
    public void testTamperedSignedTokenReturns404() throws Exception {
        runner.run(context -> {
            FileEntryTokens fileEntryTokens = context.getBean(FileEntryTokens.class);
            FileEntry fileEntry = new FileEntry(
                "report.txt", "txt", "text/plain", "file:/temp/3a2b1c4d-5e6f-7081-9203-405060708090.txt");

            String token = fileEntryTokens.toSignedToken(fileEntry);
            String tampered = token.substring(0, token.length() - 4) + "XXXX";

            TempFileStorage tempFileStorage = mock(TempFileStorage.class);
            FileEntryController controller = new FileEntryController(tempFileStorage, fileEntryTokens, false);
            MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .build();

            mockMvc.perform(get("/file-entries/" + tampered + "/content"))
                .andExpect(status().isNotFound());
        });
    }
}
