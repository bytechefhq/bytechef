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

package com.bytechef.rest.error;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

/**
 * @author Ivica Cardic
 */
public class BadRequestAlertException extends ErrorResponseException {

    private final String entityName;
    private final String errorKey;

    public BadRequestAlertException(URI type, String defaultMessage, String entityName, String errorKey) {
        super(HttpStatus.BAD_REQUEST, asProblemDetail(type, defaultMessage, entityName, errorKey), null);

        this.entityName = entityName;
        this.errorKey = errorKey;
    }

    private static ProblemDetail asProblemDetail(URI type, String message, String entityName, String errorKey) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);

        problemDetail.setTitle("Bad Request");
        problemDetail.setType(type);
        problemDetail.setProperty("message", "error." + errorKey);
        problemDetail.setProperty("params", entityName);

        return problemDetail;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getErrorKey() {
        return errorKey;
    }
}
