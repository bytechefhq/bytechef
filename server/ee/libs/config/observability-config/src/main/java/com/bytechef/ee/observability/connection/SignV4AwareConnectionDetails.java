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

package com.bytechef.ee.observability.connection;

import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingProperties;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.Transport;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * @author Igor Beslic
 */
@Component
public class SignV4AwareConnectionDetails implements OtlpLoggingConnectionDetails {
    private final OtlpLoggingProperties properties;

    SignV4AwareConnectionDetails(OtlpLoggingProperties properties) {
        this.properties = properties;
    }

    @Override
    public String getUrl(Transport transport) {
        Assert.state(transport == this.properties.getTransport(),
            "Requested transport %s doesn't match configured transport %s".formatted(transport,
                this.properties.getTransport()));

        String endpoint = this.properties.getEndpoint();
        Assert.state(endpoint != null, "'endpoint' must not be null");
        return endpoint;
    }

}
