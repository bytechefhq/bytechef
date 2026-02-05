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

package com.bytechef.platform.component.log.domain;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/**
 * Represents a single log entry from component execution.
 *
 * @author Ivica Cardic
 */
public record LogEntry(
    Instant timestamp,
    Level level,
    String componentName,
    @Nullable String componentOperationName,
    long taskExecutionId,
    String message,
    @Nullable String exceptionType,
    @Nullable String exceptionMessage,
    @Nullable String stackTrace) {

    public enum Level {
        TRACE(0),
        DEBUG(1),
        INFO(2),
        WARN(3),
        ERROR(4);

        private final int priority;

        Level(int priority) {
            this.priority = priority;
        }

        public int getPriority() {
            return priority;
        }

        public boolean isAtLeast(Level other) {
            return this.priority >= other.priority;
        }
    }

    public LogEntry {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private Instant timestamp;
        private Level level;
        private String componentName;
        private String componentOperationName;
        private long taskExecutionId;
        private String message;
        private String exceptionType;
        private String exceptionMessage;
        private String stackTrace;

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;

            return this;
        }

        public Builder level(Level level) {
            this.level = level;

            return this;
        }

        public Builder componentName(String componentName) {
            this.componentName = componentName;

            return this;
        }

        public Builder componentOperationName(String componentOperationName) {
            this.componentOperationName = componentOperationName;

            return this;
        }

        public Builder taskExecutionId(long taskExecutionId) {
            this.taskExecutionId = taskExecutionId;

            return this;
        }

        public Builder message(String message) {
            this.message = message;

            return this;
        }

        public Builder exception(Exception exception) {
            if (exception != null) {
                this.exceptionType = exception.getClass()
                    .getName();
                this.exceptionMessage = exception.getMessage();
                this.stackTrace = getStackTraceAsString(exception);
            }

            return this;
        }

        public LogEntry build() {
            return new LogEntry(
                timestamp, level, componentName, componentOperationName,
                taskExecutionId, message, exceptionType, exceptionMessage, stackTrace);
        }

        @SuppressFBWarnings("INFORMATION_EXPOSURE_THROUGH_AN_ERROR_MESSAGE")
        private static String getStackTraceAsString(Exception exception) {
            StringWriter stringWriter = new StringWriter();

            exception.printStackTrace(new PrintWriter(stringWriter));

            return stringWriter.toString();
        }
    }
}
