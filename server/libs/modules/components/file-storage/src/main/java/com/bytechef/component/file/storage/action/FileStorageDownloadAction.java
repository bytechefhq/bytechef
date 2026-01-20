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

package com.bytechef.component.file.storage.action;

import static com.bytechef.component.definition.ComponentDsl.action;
import static com.bytechef.component.definition.ComponentDsl.fileEntry;
import static com.bytechef.component.definition.ComponentDsl.outputSchema;
import static com.bytechef.component.definition.ComponentDsl.string;
import static com.bytechef.component.file.storage.constant.FileStorageConstants.FILENAME;

import com.bytechef.component.definition.ActionContext;
import com.bytechef.component.definition.ComponentDsl.ModifiableActionDefinition;
import com.bytechef.component.definition.FileEntry;
import com.bytechef.component.definition.Parameters;
import com.bytechef.component.file.storage.constant.FileStorageConstants;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Consumer;

/**
 * @author Ivica Cardic
 */
public class FileStorageDownloadAction {

    public static final ModifiableActionDefinition ACTION_DEFINITION = action("download")
        .title("Download File")
        .description("Download a file from the URL.")
        .properties(
            string(FileStorageConstants.URL)
                .label("URL")
                .description("The URL to download a file from.")
                .required(true),
            string(FILENAME)
                .label("Filename")
                .description("Filename to set for data. By default, \"file.txt\" will be used.")
                .defaultValue("file.txt"))
        .output(outputSchema(fileEntry().description("Downloaded file.")))
        .perform(FileStorageDownloadAction::perform);

    /**
     * performs the download of a file (given its URL).
     */
    protected static FileEntry perform(
        Parameters inputParameters, Parameters connectionParameters, ActionContext context) throws IOException {

        URL url = toURL(inputParameters.getRequiredString(FileStorageConstants.URL));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.connect();

        if (connection.getResponseCode() / 100 == 2) {
            File downloadedFile = File.createTempFile("download-", "", null);
            int contentLength = connection.getContentLength();

            try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
                OutputStream outputStream = new ProgressingOutputStream(
                    new FileOutputStream(downloadedFile), contentLength, progress -> context.event(
                        event -> event.publishActionProgressEvent(progress)))) {
                copy(inputStream, outputStream);
            }

            try (FileInputStream fileInputStream = new FileInputStream(downloadedFile)) {
                return context.file(file -> file.storeContent(
                    inputParameters.getRequiredString(FILENAME), fileInputStream));
            }
        }

        throw new IllegalStateException("Server returned responseCode=%s".formatted(connection.getResponseCode()));
    }

    private static long copy(final InputStream inputStream, final OutputStream outputStream) throws IOException {
        byte[] buffer = new byte[8192];
        long count = 0;
        int n;

        while (-1 != (n = inputStream.read(buffer))) {
            outputStream.write(buffer, 0, n);

            count += n;
        }

        return count;
    }

    private static URL toURL(String fileUrl) {
        try {
            URI uri = new URI(fileUrl);

            uri = uri.parseServerAuthority();

            return uri.toURL();
        } catch (URISyntaxException | MalformedURLException e) {
            throw new IllegalArgumentException("Unable to create url: =%s".formatted(fileUrl), e);
        }
    }

    private static final class ProgressingOutputStream extends FilterOutputStream {

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

        private void report() {
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
