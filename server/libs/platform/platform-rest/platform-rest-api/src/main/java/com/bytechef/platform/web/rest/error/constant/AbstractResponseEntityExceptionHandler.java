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

package com.bytechef.platform.web.rest.error.constant;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures. The error response
 * follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 *
 * @author Ivica Cardic
 */
public abstract class AbstractResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    protected ProblemDetail createProblemDetail(
        Exception exception, HttpStatus status, Class<?> entityClass, int errorKey, String errorMessageCode,
        List<?> errorMessageArguments, Map<String, ?> properties, WebRequest request) {

        ProblemDetail problemDetail = createProblemDetail(
            exception, status, exception.getMessage(), errorMessageCode,
            errorMessageArguments == null ? null : errorMessageArguments.toArray(), request);

        problemDetail.setTitle("Error");
        problemDetail.setType(URI.create(ErrorConstants.PROBLEM_BASE_URL + "/" + errorMessageCode));
        problemDetail.setProperty("entityClass", entityClass.getSimpleName());
        problemDetail.setProperty("errorKey", errorKey);

        if (properties != null && !properties.isEmpty()) {
            for (Map.Entry<String, ?> entry : properties.entrySet()) {
                problemDetail.setProperty(entry.getKey(), entry.getValue());
            }
        }

        return problemDetail;
    }
}
