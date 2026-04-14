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

package com.bytechef.ee.observability.autoconfigure;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporterBuilder;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingConnectionDetails;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingProperties;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.Transport;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@AutoConfiguration
@AutoConfigureBefore(OtlpLoggingAutoConfiguration.class)
@ConditionalOnClass({
    OpenTelemetry.class, SdkLoggerProvider.class
})
@EnableConfigurationProperties(OtlpLoggingProperties.class)
public class OtlpAutoConfiguration {

    @Bean
    @ConditionalOnBean(OtlpLoggingConnectionDetails.class)
    OtlpHttpLogRecordExporter otlpHttpLogRecordExporter(
        OtlpLoggingProperties properties, OtlpLoggingConnectionDetails connectionDetails) {

        OtlpHttpLogRecordExporterBuilder builder = OtlpHttpLogRecordExporter.builder()
            .setEndpoint(connectionDetails.getUrl(Transport.HTTP))
            .setTimeout(properties.getTimeout())
            .setConnectTimeout(properties.getConnectTimeout());

        if (properties.getCompression() != null) {
            builder.setCompression(properties.getCompression()
                .name()
                .toLowerCase());
        }

        properties.getHeaders()
            .forEach(builder::addHeader);

        String endpoint = connectionDetails.getUrl(Transport.HTTP);

        if (endpoint.contains("amazonaws.com")) {
            builder.addHeader("Authorization",
                "AWS4-HMAC-SHA256Credential=AKIAIOSFODNN7EXAMPLE/20220830/us-east-1/ec2/aws4_request,SignedHeaders=host;x-amz-date,Signature=calculated-signature");
        }

        return builder.build();
    }

    @Component
    static class SignV4AwareConnectionDetails implements OtlpLoggingConnectionDetails {
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

}
