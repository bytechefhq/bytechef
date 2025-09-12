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

package com.bytechef.platform.githubproxy.client.config;

import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import com.bytechef.platform.githubproxy.client.internal.RestGitHubProxyClient;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GitHubProxyClientProperties.class)
class GitHubProxyClientConfiguration {

    @Bean
    GitHubProxyClient gitHubProxyClient(GitHubProxyClientProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

        RestClient restClient = RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(new JdkClientHttpRequestFactory(httpClient))
            .build();

        return new RestGitHubProxyClient(restClient);
    }
}
