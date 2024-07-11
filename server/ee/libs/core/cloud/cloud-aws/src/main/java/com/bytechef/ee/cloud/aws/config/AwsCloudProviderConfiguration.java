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

package com.bytechef.ee.cloud.aws.config;

import com.bytechef.config.ApplicationProperties;
import com.bytechef.ee.cloud.aws.auth.credentials.CustomAWSCredentialsProvider;
import com.bytechef.ee.cloud.aws.regions.providers.CustomAwsRegionProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;

@Configuration
@ConditionalOnProperty(prefix = "bytechef", name = "cloud.provider", havingValue = "aws")
public class AwsCloudProviderConfiguration {

    private final ApplicationProperties.Cloud.Aws aws;

    public AwsCloudProviderConfiguration(ApplicationProperties applicationProperties) {
        this.aws = applicationProperties.getCloud()
            .getAws();
    }

    @Bean
    public AwsCredentialsProvider customAwsCredentialsProvider() {
        return new CustomAWSCredentialsProvider(aws);
    }

    @Bean
    public AwsRegionProvider customRegionProvider() {
        return new CustomAwsRegionProvider(aws);
    }
}
