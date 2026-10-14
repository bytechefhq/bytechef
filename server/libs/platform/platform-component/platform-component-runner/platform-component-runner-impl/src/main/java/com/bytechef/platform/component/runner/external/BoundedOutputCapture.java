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

package com.bytechef.platform.component.runner.external;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

/**
 * @author Ivica Cardic
 */
public final class BoundedOutputCapture {

    private static final long DEFAULT_MAX_BYTES = 64L * 1024;

    private final int halfLimit;
    private final ByteArrayOutputStream head = new ByteArrayOutputStream();
    private final byte[] tail;

    private int tailLength;
    private int tailPosition;
    private long totalLength;

    public BoundedOutputCapture() {
        this(DEFAULT_MAX_BYTES);
    }

    public BoundedOutputCapture(long maxBytes) {
        if (maxBytes < 2) {
            throw new IllegalArgumentException("maxBytes must be at least 2");
        }

        this.halfLimit = (int) Math.min(maxBytes / 2, Integer.MAX_VALUE - 8);
        this.tail = new byte[halfLimit];
    }

    public synchronized void append(byte[] buffer, int length) {
        for (int index = 0; index < length; index++) {
            byte value = buffer[index];

            if (head.size() < halfLimit) {
                head.write(value);
            } else {
                tail[tailPosition] = value;
                tailPosition = (tailPosition + 1) % halfLimit;

                if (tailLength < halfLimit) {
                    tailLength++;
                }
            }
        }

        totalLength += length;
    }

    public synchronized String get() {
        byte[] headBytes = head.toByteArray();

        if (tailLength == 0) {
            return decode(headBytes);
        }

        byte[] tailBytes = orderedTail();

        long elided = totalLength - headBytes.length - tailLength;

        if (elided <= 0) {
            return decode(concatenate(headBytes, tailBytes));
        }

        return decode(headBytes) + "%n... %d bytes elided ...%n".formatted(elided) + decode(tailBytes);
    }

    public synchronized long getTotalLength() {
        return totalLength;
    }

    private static byte[] concatenate(byte[] first, byte[] second) {
        byte[] combined = new byte[first.length + second.length];

        System.arraycopy(first, 0, combined, 0, first.length);
        System.arraycopy(second, 0, combined, first.length, second.length);

        return combined;
    }

    private byte[] orderedTail() {
        byte[] ordered = new byte[tailLength];

        int start = tailLength < halfLimit ? 0 : tailPosition;

        for (int index = 0; index < tailLength; index++) {
            ordered[index] = tail[(start + index) % halfLimit];
        }

        return ordered;
    }

    private static String decode(byte[] bytes) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE);

        try {
            CharBuffer charBuffer = decoder.decode(ByteBuffer.wrap(bytes));

            return charBuffer.toString();
        } catch (CharacterCodingException exception) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
