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

package com.bytechef.platform.ai.a2a;

import java.util.List;
import java.util.Objects;

/**
 * @author Ivica Cardic
 */
public record A2AAgentDescriptor(
    String name, String description, String url, String version, List<A2ASkill> skills) {

    public A2AAgentDescriptor {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(version, "version");

        description = description == null ? "" : description;
        skills = skills == null ? List.of() : List.copyOf(skills);
    }

    public record A2ASkill(String id, String name, String description, List<String> tags) {

        public A2ASkill {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");

            description = description == null ? "" : description;
            tags = tags == null ? List.of() : List.copyOf(tags);
        }
    }
}
