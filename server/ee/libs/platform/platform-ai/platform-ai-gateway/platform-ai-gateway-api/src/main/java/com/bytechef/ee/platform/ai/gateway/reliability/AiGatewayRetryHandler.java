/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.reliability;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayModelDeployment;
import java.util.List;
import java.util.function.Function;
import reactor.core.publisher.Flux;

/**
 * @version ee
 */
public interface AiGatewayRetryHandler {

    <T> T executeWithRetry(
        List<AiGatewayModelDeployment> deployments, Function<AiGatewayModelDeployment, T> action);

    <T> Flux<T> executeStreamWithRetry(
        List<AiGatewayModelDeployment> deployments,
        Function<AiGatewayModelDeployment, Flux<T>> action);
}
