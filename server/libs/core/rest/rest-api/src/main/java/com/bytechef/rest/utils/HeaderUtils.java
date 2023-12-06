/*
 * Copyright 2023-present ByteChef Inc.
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

package com.bytechef.rest.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * @author Ivica Cardic
 */
public class HeaderUtils {

    private static final Logger logger = LoggerFactory.getLogger(HeaderUtils.class);

    private HeaderUtils() {
    }

    /**
     * <p>
     * createFailureAlert.
     * </p>
     *
     * @param applicationName   a {@link String} object.
     * @param enableTranslation a boolean.
     * @param entityName        a {@link String} object.
     * @param errorKey          a {@link String} object.
     * @param defaultMessage    a {@link String} object.
     * @return a {@link org.springframework.http.HttpHeaders} object.
     */
    public static HttpHeaders createFailureAlert(
        String applicationName,
        boolean enableTranslation,
        String entityName,
        String errorKey,
        String defaultMessage) {
        logger.error("Entity processing failed, {}", defaultMessage);

        String message = enableTranslation ? "error." + errorKey : defaultMessage;

        HttpHeaders headers = new HttpHeaders();

        headers.add("X-" + applicationName + "-error", message);
        headers.add("X-" + applicationName + "-params", entityName);

        return headers;
    }
}
