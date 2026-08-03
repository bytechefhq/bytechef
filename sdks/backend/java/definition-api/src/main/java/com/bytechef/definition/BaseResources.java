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

package com.bytechef.definition;

import java.util.Map;

/**
 * Represents a collection of reference resources associated with a component, such as its primary documentation URL and
 * any supplementary links.
 *
 * @author Ivica Cardic
 */
public interface BaseResources {

    /**
     * Returns the primary documentation URL for the associated component.
     *
     * @return the documentation URL
     */
    String getDocumentationUrl();

    /**
     * Returns the optional supplementary links, keyed by a descriptive label.
     *
     * @return a map of labels to URLs, or an empty map if none are defined
     */
    default Map<String, String> getAdditionalUrls() {
        return Map.of();
    }
}
