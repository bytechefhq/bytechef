/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.remote.grpc.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.remote.grpc.EchoService;
import com.bytechef.ee.remote.grpc.InProcessGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 */
class DeadlineClientInterceptorTest {

    @Test
    void testCallThatNeverAnswersExceedsTheDeadline() throws Exception {
        try (InProcessGrpc inProcessGrpc = new InProcessGrpc(
            new EchoService(), List.of(), List.of(new DeadlineClientInterceptor(Duration.ofMillis(200))))) {

            assertThatThrownBy(() -> EchoService.neverAnswer(inProcessGrpc.channel()))
                .isInstanceOfSatisfying(StatusRuntimeException.class, exception -> assertThat(exception.getStatus()
                    .getCode()).isEqualTo(Status.Code.DEADLINE_EXCEEDED));
        }
    }

    @Test
    void testCallWithinTheDeadlineSucceeds() throws Exception {
        try (InProcessGrpc inProcessGrpc = new InProcessGrpc(
            new EchoService(), List.of(), List.of(new DeadlineClientInterceptor(Duration.ofSeconds(5))))) {

            assertThat(EchoService.echoTenant(inProcessGrpc.channel())).isNotNull();
        }
    }
}
