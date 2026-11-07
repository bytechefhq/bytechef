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

package com.bytechef.automation.assetfile.web.security.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.PathContainer;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * @author Ivica Cardic
 */
class AssetFileAuthorizeHttpRequestContributorTest {

    private final AssetFileAuthorizeHttpRequestContributor contributor =
        new AssetFileAuthorizeHttpRequestContributor();

    @Test
    void testPermitsOnlyTheTokenGatedDownloadPaths() {
        assertThat(contributor.getApiPermitAllRequestMatcherPaths()).containsExactly(
            "/api/automation/asset-files/public/**", "/api/automation/asset-files/signed/**");
    }

    @Test
    void testSignedDownloadPathIsPermittedAnonymously() {
        assertThat(isPermitted("/api/automation/asset-files/signed/v1.123.payload.sig")).isTrue();
    }

    @Test
    void testRequestsWithoutATokenStayOnTheAuthenticatedPath() {
        assertThat(isPermitted("/api/automation/internal/asset-files/5/content")).isFalse();
        assertThat(isPermitted("/api/automation/internal/asset-files/upload")).isFalse();
        assertThat(isPermitted("/api/automation/asset-files/5/content")).isFalse();
    }

    private boolean isPermitted(String path) {
        List<String> patterns = contributor.getApiPermitAllRequestMatcherPaths();
        PathPatternParser pathPatternParser = PathPatternParser.defaultInstance;

        return patterns.stream()
            .map(pathPatternParser::parse)
            .anyMatch((PathPattern pathPattern) -> pathPattern.matches(PathContainer.parsePath(path)));
    }
}
