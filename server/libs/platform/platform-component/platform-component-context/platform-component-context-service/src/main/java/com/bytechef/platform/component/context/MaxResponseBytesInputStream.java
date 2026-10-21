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

package com.bytechef.platform.component.context;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.jspecify.annotations.Nullable;

/**
 * Hands at most {@code maxResponseBytes} bytes of an HTTP response body to its reader. When the upstream has more, the
 * stream closes the upstream (which cancels the exchange and releases the connection), records that the limit was
 * exceeded and reports end of stream, so the reader finishes normally and the caller decides what to do with what was
 * stored. Mark and reset are not supported, and skipping reads through the counter.
 *
 * @author Ivica Cardic
 */
final class MaxResponseBytesInputStream extends FilterInputStream {

    private static final int SKIP_BUFFER_SIZE = 8192;

    private @Nullable IOException closeException;
    private boolean exceeded;
    private final long maxResponseBytes;
    private long readBytes;

    MaxResponseBytesInputStream(InputStream inputStream, long maxResponseBytes) {
        super(inputStream);

        this.maxResponseBytes = maxResponseBytes;
    }

    @Override
    public int available() throws IOException {
        if (exceeded) {
            return 0;
        }

        return super.available();
    }

    /**
     * The failure closing the upstream after the limit was exceeded, if closing failed.
     */
    @Nullable
    IOException getCloseException() {
        return closeException;
    }

    boolean isExceeded() {
        return exceeded;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public int read() throws IOException {
        if (exceeded) {
            return -1;
        }

        int value = super.read();

        if (value == -1) {
            return -1;
        }

        if (readBytes >= maxResponseBytes) {
            markExceeded();

            return -1;
        }

        readBytes++;

        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (exceeded) {
            return -1;
        }

        int read = super.read(buffer, offset, length);

        if (read == -1) {
            return -1;
        }

        long allowedBytes = maxResponseBytes - readBytes;

        if (read > allowedBytes) {
            markExceeded();

            if (allowedBytes <= 0) {
                return -1;
            }

            readBytes = maxResponseBytes;

            return (int) allowedBytes;
        }

        readBytes += read;

        return read;
    }

    @Override
    public long skip(long length) throws IOException {
        byte[] buffer = new byte[SKIP_BUFFER_SIZE];
        long remaining = length;

        while (remaining > 0) {
            int read = read(buffer, 0, (int) Math.min(buffer.length, remaining));

            if (read == -1) {
                break;
            }

            remaining -= read;
        }

        return length - remaining;
    }

    private void markExceeded() {
        exceeded = true;

        try {
            close();
        } catch (IOException ioException) {
            closeException = ioException;
        }
    }
}
