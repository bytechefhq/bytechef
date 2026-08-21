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

package com.bytechef.platform.component.polyglot;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * Line-buffered {@link OutputStream} that forwards guest stdout/stderr to a logger.
 *
 * <p>
 * A sandboxed context must redirect its streams away from the host's, and letting them fall on the floor would silently
 * drop every {@code console.log} and {@code print} a script author writes. Lines longer than {@link #MAX_LINE_LENGTH}
 * are flushed early so a guest cannot grow this buffer without bound.
 *
 * @author Ivica Cardic
 */
final class GuestLoggingOutputStream extends OutputStream {

    private static final int MAX_LINE_LENGTH = 8192;

    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private final Consumer<String> lineConsumer;

    GuestLoggingOutputStream(Consumer<String> lineConsumer) {
        this.lineConsumer = lineConsumer;
    }

    @Override
    public synchronized void write(int b) {
        if (b == '\n') {
            flushLine();

            return;
        }

        if (b != '\r') {
            buffer.write(b);
        }

        if (buffer.size() >= MAX_LINE_LENGTH) {
            flushLine();
        }
    }

    @Override
    public synchronized void flush() {
        flushLine();
    }

    @Override
    public synchronized void close() {
        flushLine();
    }

    private void flushLine() {
        if (buffer.size() == 0) {
            return;
        }

        String line = buffer.toString(StandardCharsets.UTF_8);

        buffer.reset();

        lineConsumer.accept(line);
    }
}
