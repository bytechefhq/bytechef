/*
 * Copyright 2021 <your company/name>.
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

package com.bytechef.hermes.component.definition;

import com.bytechef.hermes.definition.Display;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings("EI")
public final class ComponentDisplay extends Display {

    private String category;
    private String subtitle;
    private String[] tags;

    public ComponentDisplay(String label) {
        super(label);
    }

    public ComponentDisplay category(String category) {
        this.category = category;

        return this;
    }

    public ComponentDisplay description(String description) {
        this.description = description;

        return this;
    }

    public ComponentDisplay icon(String icon) {
        this.icon = icon;

        return this;
    }

    public ComponentDisplay subtitle(String subtitle) {
        this.subtitle = subtitle;

        return this;
    }

    public ComponentDisplay tags(String... tags) {
        this.tags = tags;

        return this;
    }

    public String getCategory() {
        return category;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String[] getTags() {
        return tags;
    }
}
