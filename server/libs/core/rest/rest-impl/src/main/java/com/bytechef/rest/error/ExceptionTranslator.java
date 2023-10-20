
/*
 * Copyright 2021 <your company/name>.
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

import com.bytechef.rest.error.constant.ErrorConstants;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.result.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures. The error response
 * follows RFC7807 - Problem Details for HTTP APIs (https://tools.ietf.org/html/rfc7807).
 *
 * @author Ivica Cardic
 */
@RestControllerAdvice
public class ExceptionTranslator extends ResponseEntityExceptionHandler {

    private static final Object[] DETAIL_MESSAGE_ARGUMENTS = {
        ErrorConstants.ERR_CONCURRENCY_FAILURE
    };
    private static final String MESSAGE_KEY = "message";

    @ExceptionHandler
    public Mono<ResponseEntity<ProblemDetail>> handleConcurrencyFailure(
        ConcurrencyFailureException concurrencyFailureException, ServerWebExchange serverWebExchange) {

        return Mono.just(
            ResponseEntity
                .of(
                    createProblemDetail(
                        concurrencyFailureException, HttpStatus.CONFLICT, "Concurrency Failure", MESSAGE_KEY,
                        DETAIL_MESSAGE_ARGUMENTS, serverWebExchange))
                .build());
    }
}
