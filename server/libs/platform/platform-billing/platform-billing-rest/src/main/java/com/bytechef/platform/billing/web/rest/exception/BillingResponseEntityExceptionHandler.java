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

package com.bytechef.platform.billing.web.rest.exception;

import com.bytechef.platform.billing.exception.InvalidWebhookSignatureException;
import com.bytechef.platform.billing.exception.PaymentClientException;
import com.bytechef.web.rest.error.AbstractResponseEntityExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * @author Matija Petanjek
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BillingResponseEntityExceptionHandler extends AbstractResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(BillingResponseEntityExceptionHandler.class);

    @ExceptionHandler(PaymentClientException.class)
    public ResponseEntity<ProblemDetail> handlePaymentClientException(
        final PaymentClientException exception, final WebRequest request) {

        log.error(exception.getMessage(), exception);

        return ResponseEntity
            .of(createProblemDetail(exception, HttpStatus.BAD_GATEWAY, exception.getMessage(), null, null, request))
            .build();
    }

    @ExceptionHandler(InvalidWebhookSignatureException.class)
    public ResponseEntity<ProblemDetail> handleInvalidWebhookSignatureException(
        final InvalidWebhookSignatureException exception, final WebRequest request) {

        log.error(exception.getMessage(), exception);

        return ResponseEntity
            .of(createProblemDetail(exception, HttpStatus.BAD_REQUEST, exception.getMessage(), null, null, request))
            .build();
    }
}
