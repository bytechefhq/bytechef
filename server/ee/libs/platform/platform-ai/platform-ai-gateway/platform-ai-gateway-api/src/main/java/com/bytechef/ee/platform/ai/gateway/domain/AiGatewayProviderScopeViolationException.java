/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.domain;

/**
 * @version ee
 */
public class AiGatewayProviderScopeViolationException extends RuntimeException {

    private final long providerId;
    private final long connectedUserId;
    private final long providerConnectedUserId;

    public AiGatewayProviderScopeViolationException(
        String message, long providerId, long connectedUserId, long providerConnectedUserId) {

        super(message);

        this.providerId = providerId;
        this.connectedUserId = connectedUserId;
        this.providerConnectedUserId = providerConnectedUserId;
    }

    public long getProviderId() {
        return providerId;
    }

    public long getConnectedUserId() {
        return connectedUserId;
    }

    public long getProviderConnectedUserId() {
        return providerConnectedUserId;
    }
}
