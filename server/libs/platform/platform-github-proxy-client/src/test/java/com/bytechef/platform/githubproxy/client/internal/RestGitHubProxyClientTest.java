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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;

/**
 * @author Ivica Cardic
 */
class RestGitHubProxyClientTest {

    private static final DefaultUriBuilderFactory URI_BUILDER_FACTORY = new DefaultUriBuilderFactory();

    @Test
    void testRefIsSentAsAQueryParameter() {
        assertEquals(
            "/gh/bytechefhq/bytechef-workflows/raw/workflows/ai_email_classifier/meta.json?ref=master",
            buildRawUri("bytechefhq", "bytechef-workflows", "master", "workflows/ai_email_classifier/meta.json"));
    }

    @Test
    void testSingleSegmentFilePathKeepsTheRefOutOfThePath() {
        assertEquals(
            "/gh/octocat/Hello-World/raw/README?ref=master",
            buildRawUri("octocat", "Hello-World", "master", "README"));
    }

    @Test
    void testBlankRefIsOmitted() {
        assertEquals(
            "/gh/octocat/Hello-World/raw/README",
            buildRawUri("octocat", "Hello-World", null, "README"));
        assertEquals(
            "/gh/octocat/Hello-World/raw/README",
            buildRawUri("octocat", "Hello-World", "  ", "README"));
    }

    @Test
    void testLeadingAndRepeatedSlashesInTheFilePathAreDropped() {
        assertEquals(
            "/gh/octocat/Hello-World/raw/docs/guide.md?ref=master",
            buildRawUri("octocat", "Hello-World", "master", "/docs//guide.md"));
    }

    @Test
    void testRefContainingASlashStaysOutOfThePath() {
        assertEquals(
            "/gh/octocat/Hello-World/raw/README?ref=release/1.0",
            buildRawUri("octocat", "Hello-World", "release/1.0", "README"));
    }

    private static String buildRawUri(String owner, String repo, String ref, String filePath) {
        UriBuilder uriBuilder = URI_BUILDER_FACTORY.builder()
            .path("/gh");

        URI uri = RestGitHubProxyClient.buildRawUri(uriBuilder, owner, repo, ref, filePath);

        return uri.toString();
    }
}
