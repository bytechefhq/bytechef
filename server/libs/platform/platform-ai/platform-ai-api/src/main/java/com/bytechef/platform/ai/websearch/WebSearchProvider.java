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

package com.bytechef.platform.ai.websearch;

import org.jspecify.annotations.Nullable;

/**
 * The web search services whose API keys admins manage per environment and whose tools AI agents receive.
 *
 * @author Ivica Cardic
 */
public enum WebSearchProvider {

    BRAVE("brave", "Brave Search", "Fast web search that returns ranked result snippets."),
    FIRECRAWL("firecrawl", "Firecrawl", "Web search, web page scraping and website mapping.");

    private static final String KEY_PREFIX = "ai.webSearchProvider.";

    private final String description;
    private final String label;
    private final String name;

    WebSearchProvider(String name, String label, String description) {
        this.description = description;
        this.label = label;
        this.name = name;
    }

    public static @Nullable WebSearchProvider fetchByName(String name) {
        for (WebSearchProvider webSearchProvider : values()) {
            if (webSearchProvider.name.equals(name)) {
                return webSearchProvider;
            }
        }

        return null;
    }

    public static WebSearchProvider valueOfName(String name) {
        WebSearchProvider webSearchProvider = fetchByName(name);

        if (webSearchProvider == null) {
            throw new IllegalArgumentException("No web search provider found for name: " + name);
        }

        return webSearchProvider;
    }

    public String getDescription() {
        return description;
    }

    public String getKey() {
        return KEY_PREFIX + name;
    }

    public String getLabel() {
        return label;
    }

    public String getName() {
        return name;
    }
}
