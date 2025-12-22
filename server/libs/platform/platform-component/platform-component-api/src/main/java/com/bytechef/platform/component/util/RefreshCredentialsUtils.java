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

package com.bytechef.platform.component.util;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.component.exception.ProviderException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Igor Beslic
 */
public class RefreshCredentialsUtils {

    public static boolean matches(List<Object> refreshOn, Exception exception) {
        boolean matches = false;

        if (exception instanceof ProviderException providerException
            && Objects.nonNull(providerException.getStatusCode())) {

            matches = matches(
                providerException.getStatusCode(),
                CollectionUtils.map(
                    CollectionUtils.filter(refreshOn, item -> item instanceof Integer),
                    item -> (Integer) item));
        }

        if (!matches) {
            Throwable curException = exception;

            while (Objects.nonNull(curException) && Objects.nonNull(curException.getMessage())) {
                matches = matches(
                    curException.getMessage(),
                    CollectionUtils.map(
                        CollectionUtils.filter(refreshOn, item -> item instanceof String), item -> (String) item));

                if (matches) {
                    break;
                }

                curException = curException.getCause();
            }
        }

        return matches;
    }

    /**
     * Checks if an exception message matches against any pattern in the list.
     *
     * @param message the content of exception message
     * @return true if the exception message matches against any pattern
     */
    public static boolean matches(String message, List<String> patterns) {
        for (String pattern : patterns) {
            Pattern compiledPattern = Pattern.compile(pattern, Pattern.DOTALL);

            Matcher matcher = compiledPattern.matcher(message);

            if (matcher.matches() && matcher.group(1) != null) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a http status code matches against any status code in the list.
     *
     * @param statusCode  the http status code
     * @param statusCodes list of status code cto check against
     * @return true if the status code matches against any status code in the list
     */
    private static boolean matches(int statusCode, List<Integer> statusCodes) {
        return statusCodes.contains(statusCode);
    }
}
