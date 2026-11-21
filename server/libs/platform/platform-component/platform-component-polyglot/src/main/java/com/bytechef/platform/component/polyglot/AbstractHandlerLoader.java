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

package com.bytechef.platform.component.polyglot;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import org.springframework.cache.CacheManager;

/**
 * @author Ivica Cardic
 */
public abstract class AbstractHandlerLoader<H> {

    /**
     * <b>Security Note:</b> Path traversal is intentional. The URL is derived from internal file storage written after
     * an authorized upload, not from untrusted user input.
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public H loadHandler(URL url, Enum<?> language, String cacheKey, CacheManager cacheManager) {
        try {
            return switch (language.name()) {
                case "JAVA" -> loadJavaHandler(url, cacheKey, cacheManager);
                case "JAVASCRIPT", "PYTHON" -> loadPolyglotHandler(
                    getLanguageId(language), Files.readString(toLocalPath(url)));
                default -> throw new IllegalArgumentException("Unsupported language: " + language);
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract H loadJavaHandler(URL url, String cacheKey, CacheManager cacheManager) throws IOException;

    protected abstract H loadPolyglotHandler(String languageId, String script);

    private static String getLanguageId(Enum<?> language) {
        return switch (language.name()) {
            case "JAVASCRIPT" -> "js";
            case "PYTHON" -> "python";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    @SuppressFBWarnings({
        "PATH_TRAVERSAL_IN", "URLCONNECTION_SSRF_FD"
    })
    private static Path toLocalPath(URL url) throws IOException, URISyntaxException {
        URI uri = url.toURI();

        if ("file".equals(uri.getScheme())) {
            return Paths.get(uri);
        }

        Path tempFile = Files.createTempFile("handler_source", null);

        try (InputStream inputStream = url.openStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        return tempFile;
    }
}
