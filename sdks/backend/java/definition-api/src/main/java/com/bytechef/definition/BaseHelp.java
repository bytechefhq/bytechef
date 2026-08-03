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

import java.util.Optional;

/**
 * Represents contextual help shown to the user, consisting of a body of explanatory text and an optional link to
 * further documentation.
 *
 * @author Ivica Cardic
 */
public interface BaseHelp {

    /**
     * Returns the main body of the help text.
     *
     * @return the help body
     */
    String getBody();

    /**
     * Returns the optional URL pointing to additional documentation.
     *
     * @return an {@link Optional} containing the "learn more" URL, or an empty {@link Optional} if none is set
     */
    default Optional<String> getLearnMoreUrl() {
        return Optional.empty();
    }
}
