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

package com.bytechef.platform.githubproxy.client;

import java.util.List;

/**
 * Client for the ByteChef GitHub Proxy service (GitHubController endpoints under /gh). Uses Spring RestClient under the
 * hood.
 *
 * @author Ivica Cardic
 */
public interface GitHubProxyClient {

    /**
     * List repository files via GitHub proxy.
     *
     * @param owner  GitHub owner/org
     * @param repo   repository name
     * @param ref    optional ref (branch/tag/commit). If null or blank, the proxy will use its default.
     * @param prefix optional path prefix filter
     * @return list of files
     */
    List<FileItem> listFiles(String owner, String repo, String ref, String prefix);

    /**
     * Fetches the raw content of a file from a GitHub repository via the GitHub proxy service.
     *
     * @param owner           the GitHub owner or organization
     * @param repo            the GitHub repository name
     * @param ref             an optional Git reference (branch, tag, or commit). If null or blank, the proxy will use
     *                        its default reference
     * @param filePath        the file path within the repository
     * @param ifNoneMatch     an optional HTTP ETag header value to check for file changes
     * @param ifModifiedSince an optional HTTP date header value to check for file modifications
     * @param rangeHeader     an optional HTTP Range header value for partial content retrieval
     * @return the raw content of the file as a byte array, or null if the file is not found or unchanged
     */
    byte[] getRaw(
        String owner, String repo, String ref, String filePath, String ifNoneMatch, String ifModifiedSince,
        String rangeHeader);
}
