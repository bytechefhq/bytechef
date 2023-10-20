
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

import static com.bytechef.component.filestorage.constants.FileStorageConstants.CONTENT;
import static com.bytechef.component.filestorage.constants.FileStorageConstants.DOWNLOAD;
import static com.bytechef.component.filestorage.constants.FileStorageConstants.FILENAME;
import static com.bytechef.component.filestorage.constants.FileStorageConstants.FILE_ENTRY;
import static com.bytechef.component.filestorage.constants.FileStorageConstants.FILE_STORAGE;
import static com.bytechef.component.filestorage.constants.FileStorageConstants.READ;
import static com.bytechef.component.filestorage.constants.FileStorageConstants.WRITE;
import static com.bytechef.hermes.component.definition.ComponentDSL.action;
import static com.bytechef.hermes.component.definition.ComponentDSL.component;
import static com.bytechef.hermes.component.definition.ComponentDSL.display;
import static com.bytechef.hermes.component.definition.ComponentDSL.fileEntry;
import static com.bytechef.hermes.component.definition.ComponentDSL.string;
import static org.apache.commons.io.IOUtils.copy;

import com.bytechef.component.filestorage.constants.FileStorageConstants;
import com.bytechef.hermes.component.ComponentHandler;
import com.bytechef.hermes.component.Context;
import com.bytechef.hermes.component.ExecutionParameters;
import com.bytechef.hermes.component.FileEntry;
import com.bytechef.hermes.component.definition.ComponentDefinition;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.function.Consumer;

/**
 * @author Ivica Cardic
 */
public class FileStorageComponentHandler implements ComponentHandler {

    private final ComponentDefinition componentDefinition = component(FILE_STORAGE)
        .display(display("File Storage").description("Reads and writes data from a file"))
        .actions(
            action(READ)
                .display(display("Read from file").description("Reads data from the file."))
                .properties(fileEntry(FILE_ENTRY)
                    .label("File")
                    .description(
                        "The object property which contains a reference to the file to read from.")
                    .required(true))
                .output(string())
                .perform(this::performRead),
            action(WRITE)
                .display(display("Write to file").description("Writes the data to the file."))
                .properties(
                    string(CONTENT)
                        .label("Content")
                        .description("String to write to the file.")
                        .required(true),
                    string(FILENAME)
                        .label("Filename")
                        .description(
                            "Filename to set for data. By default, \"file.txt\" will be used.")
                        .defaultValue("file.txt"))
                .output(fileEntry())
                .perform(this::performWrite),
            action(DOWNLOAD)
                .display(display("Download file").description("Download thr file from the URL."))
                .properties(
                    string(FileStorageConstants.URL)
                        .label("URL")
                        .description("The URL to download the file from.")
                        .required(true),
                    string(FILENAME)
                        .label("Filename")
                        .description(
                            "Filename to set for data. By default, \"file.txt\" will be used.")
                        .defaultValue("file.txt"))
                .output(fileEntry())
                .perform(this::performDownload));

    @Override
    public ComponentDefinition getDefinition() {
        return componentDefinition;
    }

    /**
     * Performs the download of a file (given its URL).
     */
    protected FileEntry performDownload(Context context, ExecutionParameters executionParameters) {
        try {
            URL url = new URL(executionParameters.getRequiredString(FileStorageConstants.URL));
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.connect();

            if (connection.getResponseCode() / 100 == 2) {
                File downloadedFile = File.createTempFile("download-", "", null);
                int contentLength = connection.getContentLength();

                try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
                    OutputStream outputStream = new ProgressingOutputStream(
                        new FileOutputStream(downloadedFile), contentLength, context::publishProgressEvent)) {
                    copy(inputStream, outputStream);
                }

                try (FileInputStream fileInputStream = new FileInputStream(downloadedFile)) {
                    return context.storeFileContent(executionParameters.getRequiredString(FILENAME), fileInputStream);
                }
            }

            throw new IllegalStateException("Server returned: " + connection.getResponseCode());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    protected String performRead(Context context, ExecutionParameters executionParameters) {
        return context.readFileToString(executionParameters.get(FILE_ENTRY, FileEntry.class));
    }

    protected FileEntry performWrite(Context context, ExecutionParameters executionParameters) {
        Object content = executionParameters.getRequired(CONTENT);
        String fileName = executionParameters.getString(FILENAME, "file.txt");

        return context.storeFileContent(fileName, content instanceof String ? (String) content : content.toString());
    }

    protected static final class ProgressingOutputStream extends FilterOutputStream {

        private final int totalSize;
        private final OutputStream out;
        private final Consumer<Integer> progressConsumer;
        private long count;
        private long lastTime;
        private static final long delta = 500;

        ProgressingOutputStream(OutputStream outputStream, int totalSize, Consumer<Integer> progressConsumer) {
            super(outputStream);

            this.totalSize = totalSize;
            this.out = outputStream;
            this.progressConsumer = progressConsumer;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            this.out.write(b, off, len);
            this.count += len;

            report();
        }

        @Override
        public void write(int b) throws IOException {
            this.out.write(b);
            ++this.count;

            report();
        }

        void report() {
            if (totalSize != -1 && totalSize != -0) {
                long time = System.currentTimeMillis();

                if (count == totalSize) {
                    progressConsumer.accept(100);
                }

                if (time > lastTime + delta) {
                    int p = (int) (count * 100 / totalSize);

                    progressConsumer.accept(p);

                    lastTime = time;
                }
            }
        }
    }
}
