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

package com.bytechef.server.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.bytechef.config.ApplicationProperties;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * @author Ivica Cardic
 */
class BytechefProfileWebhookDeliveryTest {

    @Test
    void testTheBytechefProfileSetsWebhookDeliveryLimitsOfAtLeastOne() throws IOException {
        List<PropertySource<?>> propertySources = new YamlPropertySourceLoader().load(
            "application-bytechef", new ClassPathResource("config/application-bytechef.yml"));

        StandardEnvironment standardEnvironment = new StandardEnvironment();

        MutablePropertySources mutablePropertySources = standardEnvironment.getPropertySources();

        propertySources.forEach(mutablePropertySources::addLast);

        ApplicationProperties.WebhookDelivery webhookDelivery = Binder.get(standardEnvironment)
            .bind("bytechef.webhook-delivery", ApplicationProperties.WebhookDelivery.class)
            .orElseThrow(() -> new AssertionError("bytechef.webhook-delivery is not set"));

        assertThat(webhookDelivery.getConcurrencyLimit()).isGreaterThanOrEqualTo(1);
        assertThat(webhookDelivery.getMaxPendingDeliveries()).isGreaterThanOrEqualTo(1);
    }
}
