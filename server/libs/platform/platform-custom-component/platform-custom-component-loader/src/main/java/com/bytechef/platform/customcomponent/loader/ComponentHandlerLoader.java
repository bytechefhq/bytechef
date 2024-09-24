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

package com.bytechef.platform.customcomponent.loader;

import com.bytechef.component.ComponentHandler;
import com.bytechef.platform.customcomponent.configuration.domain.CustomComponent.Language;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * @author Ivica Cardic
 */
public class ComponentHandlerLoader {

    public static ComponentHandler loadComponentHandler(URL url, Language language, String cacheKey) {
        try {
            return switch (language) {
                case JAVA -> loadJavaComponentHandler(url, cacheKey);
                case JAVASCRIPT, PYTHON, RUBY -> loadPolyglotComponentHandler(url, language);
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static ComponentHandler loadJavaComponentHandler(URL url, String cacheKey) throws IOException {
        try (ComponentHandlerClassLoader codeComponentHandlerClassLoader = ComponentHandlerClassLoader.of(
            url, cacheKey)) {

            return codeComponentHandlerClassLoader.loadComponentHandler();
        }
    }

    private static ComponentHandler loadPolyglotComponentHandler(URL url, Language language)
        throws URISyntaxException, IOException {

        return ComponentHandlerPolyglotEngine.load(getLanguageId(language), Files.readString(Paths.get(url.toURI())));
    }

    private static String getLanguageId(Language language) {
        return switch (language) {
//            case JAVA -> "java";
            case JAVASCRIPT -> "js";
            case PYTHON -> "python";
            case RUBY -> "ruby";
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }
}
