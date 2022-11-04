/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.component.filestorage;

import static com.bytechef.hermes.component.constants.ComponentConstants.FILENAME;
import static com.bytechef.hermes.component.constants.ComponentConstants.FILE_ENTRY;
import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.test.mock.MockContext;
import com.bytechef.hermes.component.test.mock.MockExecutionParameters;
import com.bytechef.test.jsonasssert.AssertUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
public class FileStorageComponentHandlerTest {

    private static final MockContext context = new MockContext();
    private static final FileStorageComponentHandler fileStorageComponentHandler = new FileStorageComponentHandler();

    @Test
    public void testGetComponentDefinition() {
        AssertUtils.assertEquals("definition/filestorage_v1.json", fileStorageComponentHandler.getDefinition());
    }

    @Test
    public void testPerformRead() throws Exception {
        File file = getFile();

        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set(
                FILE_ENTRY,
                context.storeFileContent(file.getName(), new FileInputStream(file))
                        .toMap());

        assertThat(fileStorageComponentHandler.performRead(context, parameters))
                .isEqualTo(Files.contentOf(file, Charset.defaultCharset()));
    }

    @Disabled
    @Test
    public void testPerformDownload() throws Exception {
        // TODO
    }

    @Test
    public void testPerformWrite() throws Exception {
        File file = getFile();

        MockExecutionParameters parameters = new MockExecutionParameters();

        parameters.set("content", Files.contentOf(file, Charset.defaultCharset()));

        FileEntry fileEntry = (FileEntry) fileStorageComponentHandler.performWrite(context, parameters);

        assertThat(context.readFileToString(fileEntry)).isEqualTo(Files.contentOf(file, Charset.defaultCharset()));

        assertThat(fileEntry.getName()).isEqualTo("file.txt");

        parameters.set(FILENAME, "test.txt");
        parameters.set("content", Files.contentOf(file, Charset.defaultCharset()));

        fileEntry = (FileEntry) fileStorageComponentHandler.performWrite(context, parameters);

        assertThat(fileEntry.getName()).isEqualTo("test.txt");
    }

    private File getFile() throws IOException {
        ClassPathResource classPathResource = new ClassPathResource("dependencies/sample.txt");

        return classPathResource.getFile();
    }
}
