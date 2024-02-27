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

package com.bytechef.platform.configuration.web.rest.error;

import com.bytechef.platform.component.exception.ComponentExecutionException;
import com.bytechef.rest.error.constant.ErrorConstants;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.net.URI;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ConfigurationlResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationlResponseEntityExceptionHandler.class);

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ComponentExecutionException.class)
    @SuppressFBWarnings("BC_UNCONFIRMED_CAST")
    public ResponseEntity<Object> handleComponentExecutionException(
        final ComponentExecutionException exception, final WebRequest request) {

        if (logger.isDebugEnabled()) {
            logger.debug(exception.getMessage(), exception);
        }

        return ResponseEntity
            .of(createProblemDetail(
                exception.getCause() == null ? exception : (Exception) exception.getCause(), exception.getEntityClass(),
                exception.getErrorKey(), exception.getInputParameters(), request))
            .build();
    }

    private ProblemDetail createProblemDetail(
        Exception exception, Class<?> entityClass, int errorKey, Map<String, ?> inputParameters, WebRequest request) {

        ProblemDetail problemDetail = createProblemDetail(
            exception, HttpStatus.BAD_REQUEST, exception.getMessage(), null, null, request);

        problemDetail.setTitle("Bad Request");

        String code = StringUtils.uncapitalize(entityClass.getSimpleName()) + "-" + errorKey;

        problemDetail.setType(URI.create(ErrorConstants.PROBLEM_BASE_URL + "/" + code));
        problemDetail.setProperty("message", "error." + code);

        if (inputParameters != null && !inputParameters.isEmpty()) {
            problemDetail.setProperty("inputParameters", inputParameters);
        }

        return problemDetail;
    }
}
