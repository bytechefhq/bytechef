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

package com.bytechef.platform.githubproxy.client.internal;

import com.bytechef.platform.githubproxy.client.FileItem;
import com.bytechef.platform.githubproxy.client.GitHubProxyClient;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

/**
 * @author Ivica Cardic
 */
public class RestGitHubProxyClient implements GitHubProxyClient {

    private final RestClient restClient;

    public RestGitHubProxyClient(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
    }

    @Override
    public List<FileItem> listFiles(String owner, String repo, String ref, String prefix) {
        List<FileItem> result = restClient.get()
            .uri(uriBuilder -> buildListFilesUri(uriBuilder, owner, repo, ref, prefix))
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        return result != null ? result : List.of();
    }

    @Override
    public byte[] getRaw(
        String owner, String repo, String ref, String filePath, String ifNoneMatch, String ifModifiedSince,
        String rangeHeader) {

        RestClient.RequestHeadersSpec<?> spec = restClient.get()
            .uri(uriBuilder -> buildRawUri(uriBuilder, owner, repo, ref, filePath))
            .accept(MediaType.ALL);

        if (StringUtils.hasText(ifNoneMatch)) {
            spec = spec.header("If-None-Match", ifNoneMatch);
        }

        if (StringUtils.hasText(ifModifiedSince)) {
            spec = spec.header("If-Modified-Since", ifModifiedSince);
        }

        if (StringUtils.hasText(rangeHeader)) {
            spec = spec.header("Range", rangeHeader);
        }

        return spec.retrieve()
            .toEntity(byte[].class)
            .getBody();
    }

    private URI buildListFilesUri(UriBuilder builder, String owner, String repo, String ref, String prefix) {
        if (StringUtils.hasText(ref)) {
            builder.queryParam("ref", ref);
        }

        if (StringUtils.hasText(prefix)) {
            builder.queryParam("prefix", prefix);
        }

        return builder.path("/{owner}/{repo}/files")
            .build(owner, repo);
    }

    private URI buildRawUri(UriBuilder builder, String owner, String repo, String ref, String filePath) {
        List<String> segments = new ArrayList<>();

        segments.add(owner);
        segments.add(repo);
        segments.add("raw");

        if (StringUtils.hasText(ref)) {
            segments.add(ref);
        }

        if (filePath != null && !filePath.isEmpty()) {
            for (String part : filePath.split("/")) {
                if (!part.isEmpty()) {
                    segments.add(part);
                }
            }
        }

        return builder.pathSegment(segments.toArray(String[]::new))
            .build();
    }
}
