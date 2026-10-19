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

package com.bytechef.platform.component.definition;

import com.bytechef.component.definition.ActionContext.Suspend;
import com.bytechef.platform.component.constant.MetadataConstants;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivica Cardic
 */
public class SuspendUtils {

    private static final Logger log = LoggerFactory.getLogger(SuspendUtils.class);

    private SuspendUtils() {
    }

    public static @Nullable Instant extractSuspendExpiresAt(Map<String, ?> taskExecutionMetadata) {
        if (taskExecutionMetadata.get(MetadataConstants.SUSPEND) instanceof Suspend suspend) {
            return suspend.expiresAt();
        }

        Map<?, ?> suspendMap = getSuspendMap(taskExecutionMetadata);

        if (suspendMap == null) {
            return null;
        }

        return toInstant(suspendMap.get(Suspend.EXPIRES_AT), Suspend.EXPIRES_AT);
    }

    public static @Nullable Instant extractApprovalRaisedAt(Map<String, ?> taskExecutionMetadata) {
        Map<?, ?> continueParameters = getContinueParameters(taskExecutionMetadata);

        if (continueParameters == null) {
            return null;
        }

        return toInstant(
            continueParameters.get(MetadataConstants.APPROVAL_RAISED_AT), MetadataConstants.APPROVAL_RAISED_AT);
    }

    private static @Nullable Map<?, ?> getContinueParameters(Map<String, ?> taskExecutionMetadata) {
        if (taskExecutionMetadata.get(MetadataConstants.SUSPEND) instanceof Suspend suspend) {
            return suspend.continueParameters();
        }

        Map<?, ?> suspendMap = getSuspendMap(taskExecutionMetadata);

        if (suspendMap != null && suspendMap.get(Suspend.CONTINUE_PARAMETERS) instanceof Map<?, ?> continueParameters) {
            return continueParameters;
        }

        return null;
    }

    private static @Nullable Map<?, ?> getSuspendMap(Map<String, ?> taskExecutionMetadata) {
        if (taskExecutionMetadata.get(MetadataConstants.SUSPEND) instanceof Map<?, ?> suspendMap) {
            return suspendMap;
        }

        return null;
    }

    private static @Nullable Instant toInstant(@Nullable Object value, String key) {
        if (value instanceof Number number) {
            return Instant.ofEpochMilli(number.longValue());
        }

        if (value instanceof String string) {
            try {
                return Instant.parse(string);
            } catch (DateTimeParseException dateTimeParseException) {
                log.warn("Ignoring the unparseable suspend '{}' value '{}'", key, string, dateTimeParseException);
            }
        }

        return null;
    }
}
