/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.ai.gateway.provider;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProvider;
import com.bytechef.ee.platform.ai.gateway.domain.AiGatewayProviderType;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
class AiGatewayEmbeddingModelFactoryTest {

    @Test
    void testCustomerSuppliedBaseUrlIsValidated() {
        AiGatewayProvider customerProvider = newProvider(1L, AiGatewayProviderType.OPENAI);

        customerProvider.setBaseUrl("http://169.254.169.254/latest/meta-data/");

        AiGatewayEmbeddingModelFactory aiGatewayEmbeddingModelFactory = new AiGatewayEmbeddingModelFactoryImpl();

        assertThatThrownBy(() -> aiGatewayEmbeddingModelFactory.getEmbeddingModel(customerProvider))
            .isInstanceOf(IllegalArgumentException.class);
    }

    private static AiGatewayProvider newProvider(long id, AiGatewayProviderType type) {
        AiGatewayProvider provider = new AiGatewayProvider("byok-ssrf-test-provider", type, "sk-test-key");

        try {
            Field idField = AiGatewayProvider.class.getDeclaredField("id");

            idField.setAccessible(true);
            idField.set(provider, id);
        } catch (ReflectiveOperationException reflectiveOperationException) {
            throw new AssertionError("failed to seed id on test provider", reflectiveOperationException);
        }

        return provider;
    }
}
